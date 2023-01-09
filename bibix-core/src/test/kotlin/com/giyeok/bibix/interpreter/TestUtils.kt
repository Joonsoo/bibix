package com.giyeok.bibix.interpreter

import com.giyeok.bibix.base.*
import com.giyeok.bibix.interpreter.coroutine.Memo
import com.giyeok.bibix.interpreter.expr.EvaluationResult
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.giyeok.bibix.repo.BibixRepoProto
import com.giyeok.bibix.repo.Repo
import com.giyeok.bibix.runner.RunConfigProto
import java.nio.file.FileSystem

fun testRepo(fs: FileSystem): Repo = Repo(
  fs,
  fs.getPath("/"),
  fs.getPath("/"),
  RunConfigProto.RunConfig.getDefaultInstance(),
  fs.getPath("/"),
  BibixRepoProto.BibixRepo.newBuilder(),
  fs.getPath("/"),
  fs.getPath("/"),
  fs.getPath("/"),
  fs.getPath("/"),
  mutableMapOf()
)

fun testInterpreter(
  fs: FileSystem,
  mainPath: String,
  preloadedPlugins: Map<String, PreloadedPlugin>
) = BibixInterpreter(
  BuildEnv(OS.Linux("", ""), Architecture.X86_64),
  preloadedPlugins,
  object : Memo {
    override suspend fun memoize(
      sourceId: SourceId,
      exprId: Int,
      thisValue: BibixValue?,
      eval: suspend () -> EvaluationResult
    ): EvaluationResult = eval()
  },
  BibixProject(fs.getPath(mainPath), null),
  testRepo(fs),
  FakeProgressIndicatorContainer(),
  listOf()
)
