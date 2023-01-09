package com.giyeok.bibix.source

import com.giyeok.bibix.BibixIdProto
import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.*
import com.giyeok.bibix.repo.Repo
import com.giyeok.bibix.interpreter.coroutine.ProgressIndicator
import com.giyeok.bibix.sourceId
import java.nio.file.Path
import java.util.concurrent.locks.Lock
import kotlin.io.path.readText

// 지원되는 source 종류:
// "path/to/source/on/local"
// git(url: string, branch?: string, tag?: string, commitId?: string, path: string = "/", buildFile: string = "build.bbx")
class ImportSourceResolver(
  val repo: Repo,
  private val locks: MutableMap<BibixIdProto.SourceId, Lock> = mutableMapOf(),
) {
  fun resolveImportSourceCall(
    spec: ClassInstanceValue,
    progressIndicator: ProgressIndicator
  ): ImportedSource {
    TODO()
//    when (spec.className) {
//      CName(BibixRootSourceId, "GitSource") -> {
//        // TODO remote의 ref를 pull해오도록 수정
//        val url = (spec["url"] as StringValue).value
//        val ref = (spec["ref"] as StringValue).value
//        val path = (spec["path"] as StringValue).value
//        progressIndicator.updateProgressDescription("Resolving git source from $url @ $ref")
//
//        val sourceId = sourceId {
//          this.remoteSource = remoteSourceId {
//            this.sourceType = "git"
//            this.sourceSpec = spec.toProto()
//          }
//        }
//
//        val lock = synchronized(this) {
//          locks.getOrPut(sourceId) { ReentrantLock() }
//        }
//        // 두 곳 이상에서 같은 git repo를 건드리지 않도록
//        lock.withLock {
//          val srcDirectory = repo.prepareSourceDirectory(sourceId)
//          val srcDirectoryFile = srcDirectory.toFile()
//
//          // TODO credentialsProvider
//          val credentialsProvider = CredentialsProvider.getDefault()
//
//          val existingRepo = try {
//            Git.open(srcDirectoryFile)
//          } catch (e: IOException) {
//            null
//          }
//          val pulledRepo = if (existingRepo == null) null else {
//            progressIndicator.updateProgressDescription("Pulling from the existing repository $url @ $ref")
//            val remotes = existingRepo.remoteList().call()
//            val remoteName = remotes.find { it.urIs.contains(URIish(url)) }
//            if (remoteName == null) null else {
//              existingRepo.pull()
//                .setRemote(remoteName.name)
//                .setRemoteBranchName(ref)
//                .setCredentialsProvider(credentialsProvider)
//                .call()
//              existingRepo.checkout()
//                .setName(ref)
//                .call()
//              existingRepo
//            }
//          }
//
//          val gitRepo = if (pulledRepo != null) pulledRepo else {
//            progressIndicator.updateProgressDescription("Cloning git repository from $url @ $ref")
//            Git.cloneRepository()
//              .setURI(url)
//              .setDirectory(srcDirectoryFile)
//              .setBranch(ref)
//              .setCredentialsProvider(credentialsProvider)
//              .call()
//          }
//
//          gitRepo.checkout().setName(ref).call()
//
//          val baseDirectory = srcDirectory.resolve(path)
//          val scriptSource = baseDirectory.resolve("build.bbx").readText()
//          val parsed = BibixAst.parseAst(scriptSource)
//
//          if (parsed.isRight) {
//            throw IllegalStateException(
//              "Invalid build file from git repository $url, ${parsed.right().get()}"
//            )
//          }
//
//          return ImportedSource(sourceId, baseDirectory, parsed.left().get())
//        }
//      }
//
//      else -> TODO()
//    }
  }

  fun resolveImportSourcePath(
    mainBaseDirectory: Path,
    path: DirectoryValue,
    progressIndicator: ProgressIndicator
  ): ImportedSource {
    val baseDirectory = path.directory
    val scriptSource = baseDirectory.resolve("build.bbx").readText()
    val parsed = BibixAst.parseAst(scriptSource)

    if (parsed.isRight) {
      throw IllegalStateException(
        "Invalid build file from path ${path.directory}, ${parsed.right().get()}"
      )
    }

    val sourceId = sourceId {
      // TODO mainBaseDirectory에 대한 상대 경로로 저장
      this.localSource = path.directory.normalize().toString()
    }

    return ImportedSource(sourceId, baseDirectory, parsed.left().get())
  }
}

data class ImportedSource(
  val sourceId: BibixIdProto.SourceId,
  val baseDirectory: Path,
  val buildScript: BibixAst.BuildScript,
)
