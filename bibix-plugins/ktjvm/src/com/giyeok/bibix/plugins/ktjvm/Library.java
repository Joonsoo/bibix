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
    private BibixValue runCompiler(SetValue classPaths, SetValue pkgSet, BuildContext context) throws IOException {
        ArrayList<String> args = new ArrayList<>();
        if (!classPaths.getValues().isEmpty()) {
            ArrayList<File> cps = new ArrayList<>();
            args.add("-cp");
            // System.out.println(deps);
            classPaths.getValues().forEach(v -> {
                ((SetValue) ((ClassInstanceValue) v).getValue()).getValues().forEach(p -> {
                    cps.add(((PathValue) p).getPath());
                });
            });
            args.add(cps.stream().map(p -> {
                try {
                    return p.getCanonicalPath();
                } catch (IOException e) {
                    return "";
                }
            }).collect(Collectors.joining(":")));
        }

        File destDirectory = context.getDestDirectory();
        if (context.getHashChanged()) {
            SetValue srcs = (SetValue) context.getArguments().get("srcs");
            for (BibixValue value : srcs.getValues()) {
                args.add(((FileValue) value).getFile().getCanonicalPath());
            }

            args.add("-d");
            args.add(destDirectory.getCanonicalPath());

            args.add("-no-stdlib");
            // args.add("-no-reflect");

            // System.out.println("** ktjvm args: " + args);
            CLITool.doMain(new K2JVMCompiler(), args.toArray(new String[0]));
        }

        return new TupleValue(
                new StringValue(""),
                new SetValue(new PathValue(destDirectory)),
                pkgSet
        );
    }

    public BuildRuleReturn build(BuildContext context) throws IOException {
        ArrayList<String> args = new ArrayList<>();

        SetValue deps = (SetValue) context.getArguments().get("deps");
        return BuildRuleReturn.evalAndThen(
                "jvm.resolveClassPkgs",
                Map.of("classPkgs", deps),
                (classPaths) -> {
                    SetValue cps = (SetValue) ((ClassInstanceValue) classPaths).getValue();
                    try {
                        return BuildRuleReturn.value(runCompiler(cps, deps, context));
                    } catch (Exception e) {
                        return BuildRuleReturn.failed(e);
                    }
                });
    }

    public void run(ActionContext context) {
        System.out.println("ktjvm.Library.run " + context);
    }
}
