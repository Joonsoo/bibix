package com.giyeok.bibix.plugins.file

import com.giyeok.bibix.base.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

class Copy {
  fun copyFile(context: ActionContext) {
    val src = (context.arguments.getValue("src") as FileValue).file
    val dest = (context.arguments.getValue("dest") as PathValue).path
    val overwrite = (context.arguments.getValue("overwrite") as BooleanValue).value

    // dest가 존재하지 않고, dest.parent는 디렉토리인 경우 -> dest라는 이름으로 복사

    when {
      dest.notExists() && dest.parent.isDirectory() -> {
        src.copyTo(dest, overwrite = overwrite)
      }

      dest.isRegularFile() -> {
        check(overwrite)
        src.copyTo(dest, overwrite = true)
      }

      dest.isDirectory() -> {
        val fileDest = dest.resolve(src.fileName.name)
        src.copyTo(fileDest, overwrite = overwrite)
      }

      else -> throw IllegalStateException("Unsupported operation")
    }
  }

  fun copyFiles(context: ActionContext) {
    val srcs = (context.arguments.getValue("srcs") as SetValue).values.map { (it as FileValue).file }
    val dest = (context.arguments.getValue("dest") as PathValue).path
    val overwrite = (context.arguments.getValue("overwrite") as BooleanValue).value

    // dest가 존재하지 않고, dest.parent는 디렉토리인 경우 -> dest라는 이름으로 복사

    when {
      dest.isDirectory() -> {
        srcs.forEach { src ->
          val fileDest = dest.resolve(src.fileName.name)
          src.copyTo(fileDest, overwrite = overwrite)
        }
      }

      else -> throw IllegalStateException("Unsupported operation")
    }
  }

  fun copyDirectory(context: ActionContext) {
    val src = (context.arguments.getValue("src") as DirectoryValue).directory
    val dest = (context.arguments.getValue("dest") as DirectoryValue).directory
    // createDirectories가 true이면 src 디렉토리의 sub 디렉토리중에 dest에 없는 것이 있으면 생성
    val createDirectories = (context.arguments.getValue("createDirectories") as BooleanValue).value
    val overwrite = (context.arguments.getValue("overwrite") as BooleanValue).value

    fun copy(currSrc: Path, currDest: Path) {
      val (subdirs, subfiles) = Files.list(currSrc).toList()
        .partition { it.isDirectory() }
      // TODO symlink는 어떻게 되지? 확인 필요
      subfiles.forEach { subfile ->
        subfile.copyTo(currDest.resolve(subfile.fileName.name), overwrite = overwrite)
      }
      subdirs.forEach { subdir ->
        val destSub = currDest.resolve(subdir.fileName.name)
        check(!destSub.isRegularFile())
        if (!destSub.exists()) {
          check(createDirectories)
          destSub.createDirectory()
        }
        copy(subdir, destSub)
      }
    }
    copy(src, dest)
  }
}
