package com.giyeok.bibix.plugins.ktjvm;

import com.giyeok.bibix.base.*;
import org.jetbrains.kotlin.cli.common.CLITool;
import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

public class Library {
    private BibixValue runCompiler(SetValue classPaths, SetValue pkgSet, BuildContext context, ListValue optIns) throws IOException {
        Path destDirectory = context.getDestDirectory();

        SetValue srcs = (SetValue) context.getArguments().get("srcs");
        if (srcs.getValues().isEmpty()) {
            throw new IllegalArgumentException("srcs must not be empty");
        }

        if (context.getHashChanged()) {
            ArrayList<String> args = new ArrayList<>();
            if (!classPaths.getValues().isEmpty()) {
                ArrayList<Path> cps = new ArrayList<>();
                args.add("-cp");
                // System.out.println(deps);
                classPaths.getValues().forEach(v -> {
                    cps.add(((PathValue) v).getPath());
                });
                args.add(cps.stream().map(Path::toAbsolutePath).map(Path::toString).collect(Collectors.joining(":")));
            }

            for (BibixValue value : srcs.getValues()) {
                args.add(((FileValue) value).getFile().toAbsolutePath().toString());
            }

            args.add("-d");
            args.add(destDirectory.toAbsolutePath().toString());

            if (optIns != null && !optIns.getValues().isEmpty()) {
                optIns.getValues().forEach(optIn ->
                        args.add("-opt-in=" + ((StringValue) optIn).getValue()));
            }

            args.add("-no-stdlib");
            // args.add("-no-reflect");

            // System.out.println("** ktjvm args: " + args);
            ExitCode exitCode = CLITool.doMainNoExit(new K2JVMCompiler(), args.toArray(new String[0]));
            if (exitCode != ExitCode.OK) {
                throw new IllegalStateException("Failed to compile kotlin sources");
            }
        }

        // ClassPkg = (origin: ClassOrigin, cpinfo: CpInfo, deps: set<ClassPkg>)
        // CpInfo = {JarInfo, ClassesInfo}
        // ClassesInfo = (classDirs: set<directory>, resDirs: set<directory>, srcs: {set<file>, none})
        return new NDataClassInstanceValue("jvm.ClassPkg", Map.of(
                "origin", new NDataClassInstanceValue("jvm.LocalBuilt", Map.of(
                        "objHash", new StringValue(context.getObjectIdHash()),
                        "builderName", new StringValue("ktjvm.library")
                )),
                "cpinfo", new NDataClassInstanceValue("jvm.ClassesInfo", Map.of(
                        "classDirs", new SetValue(new DirectoryValue(destDirectory)),
                        "resDirs", new SetValue(),
                        "srcs", srcs
                ))
                // TODO srcs
        ));
    }

    public BuildRuleReturn build(BuildContext context) throws IOException {
        SetValue deps = (SetValue) context.getArguments().get("deps");
        ListValue optIns = (ListValue) context.getArguments().get("optIns");
        return BuildRuleReturn.evalAndThen(
                "jvm.resolveClassPkgs",
                Map.of("classPkgs", deps),
                (classPaths) -> {
                    SetValue cps = (SetValue) ((DataClassInstanceValue) classPaths).get("cps");
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
