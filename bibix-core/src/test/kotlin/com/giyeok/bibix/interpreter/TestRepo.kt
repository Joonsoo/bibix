package com.giyeok.bibix.interpreter

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
