package com.giyeok.bibix.plugins.ktjvm;

import com.giyeok.bibix.base.*;
import org.jetbrains.kotlin.cli.common.CLITool;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

public class Library {
    private BibixValue runCompiler(SetValue classPaths, SetValue pkgSet, BuildContext context, ListValue optIns) throws IOException {
        File destDirectory = context.getDestDirectory();

        if (context.getHashChanged()) {
            ArrayList<String> args = new ArrayList<>();
            if (!classPaths.getValues().isEmpty()) {
                ArrayList<File> cps = new ArrayList<>();
                args.add("-cp");
                // System.out.println(deps);
                classPaths.getValues().forEach(v -> {
                    cps.add(((PathValue) v).getPath());
                });
                args.add(cps.stream().map(p -> {
                    try {
                        return p.getCanonicalPath();
                    } catch (IOException e) {
                        return "";
                    }
                }).collect(Collectors.joining(":")));
            }

            SetValue srcs = (SetValue) context.getArguments().get("srcs");
            for (BibixValue value : srcs.getValues()) {
                args.add(((FileValue) value).getFile().getCanonicalPath());
            }

            args.add("-d");
            args.add(destDirectory.getCanonicalPath());

            if (optIns != null && !optIns.getValues().isEmpty()) {
                optIns.getValues().forEach(optIn ->
                        args.add("-opt-in=" + ((StringValue) optIn).getValue()));
            }

            args.add("-no-stdlib");
            // args.add("-no-reflect");

            // System.out.println("** ktjvm args: " + args);
            CLITool.doMain(new K2JVMCompiler(), args.toArray(new String[0]));
        }

        // ClassPkg = (origin: ClassOrigin, cps: set<path>, deps: set<ClassPkg>)
        return new TupleValue(
                new StringValue("built by ktjvm.library: " + context.getObjectIdHash()),
                new SetValue(new PathValue(destDirectory)),
                pkgSet
        );
    }

    public BuildRuleReturn build(BuildContext context) throws IOException {
        SetValue deps = (SetValue) context.getArguments().get("deps");
        ListValue optIns = (ListValue) context.getArguments().get("optIns");
        return BuildRuleReturn.evalAndThen(
                "jvm.resolveClassPkgs",
                Map.of("classPkgs", deps),
                (classPaths) -> {
                    SetValue cps = (SetValue) ((ClassInstanceValue) classPaths).getValue();
                    try {
                        return BuildRuleReturn.value(runCompiler(cps, deps, context, optIns));
                    } catch (Exception e) {
                        return BuildRuleReturn.failed(e);
                    }
                });
    }

    public void run(ActionContext context) {
        System.out.println("ktjvm.Library.run " + context);
    }
}
