package com.giyeok.bibix.runner

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.*
import com.giyeok.bibix.utils.toProto
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.URIish
import java.io.File
import java.io.IOException
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

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
    when (spec.className) {
      CName(BibixRootSourceId, "GitSource") -> {
        // TODO remote의 ref를 pull해오도록 수정
        val specValue = spec.value as NamedTupleValue
        val url = (specValue.getValue("url") as StringValue).value
        val ref = (specValue.getValue("ref") as StringValue).value
        val path = (specValue.getValue("path") as StringValue).value
        progressIndicator.updateProgressDescription("Resolving git source from $url @ $ref")

        val sourceId = sourceId {
          this.remoteSource = remoteSourceId {
            this.sourceType = "git"
            this.sourceSpec = specValue.toProto()
          }
        }

        val lock = synchronized(this) {
          locks.getOrPut(sourceId) { ReentrantLock() }
        }
        // 두 곳 이상에서 같은 git repo를 건드리지 않도록
        lock.withLock {
          val destDirectory = repo.prepareSourceDirectory(sourceId)

          // TODO credentialsProvider
          val credentialsProvider = CredentialsProvider.getDefault()

          val existingRepo = try {
            Git.open(destDirectory)
          } catch (e: IOException) {
            null
          }
          val pulledRepo = if (existingRepo == null) null else {
            progressIndicator.updateProgressDescription("Pulling from the existing repository $url @ $ref")
            val remotes = existingRepo.remoteList().call()
            val remoteName = remotes.find { it.urIs.contains(URIish(url)) }
            if (remoteName == null) null else {
              existingRepo.pull()
                .setRemote(remoteName.name)
                .setRemoteBranchName(ref)
                .setCredentialsProvider(credentialsProvider)
                .call()
              existingRepo.checkout()
                .setName(ref)
                .call()
              existingRepo
            }
          }

          val gitRepo = if (pulledRepo != null) pulledRepo else {
            progressIndicator.updateProgressDescription("Cloning git repository from $url @ $ref")
            Git.cloneRepository()
              .setURI(url)
              .setDirectory(destDirectory)
              .setBranch(ref)
              .setCredentialsProvider(credentialsProvider)
              .call()
          }

          gitRepo.checkout().setName(ref).call()

          val baseDirectory = File(destDirectory, path)
          val scriptSource = File(baseDirectory, "build.bbx").readText()
          val parsed = BibixAst.parseAst(scriptSource)

          if (parsed.isRight) {
            throw IllegalStateException(
              "Invalid build file from git repository $url, ${parsed.right().get()}"
            )
          }

          return ImportedSource(sourceId, baseDirectory, parsed.left().get())
        }
      }
      else -> TODO()
    }
  }

  fun resolveImportSourcePath(
    mainBaseDirectory: File,
    path: DirectoryValue,
    progressIndicator: ProgressIndicator
  ): ImportedSource {
    val baseDirectory = path.directory
    val scriptSource = File(baseDirectory, "build.bbx").readText()
    val parsed = BibixAst.parseAst(scriptSource)

    if (parsed.isRight) {
      throw IllegalStateException(
        "Invalid build file from path ${path.directory}, ${parsed.right().get()}"
      )
    }

    val sourceId = sourceId {
      // TODO mainBaseDirectory에 대한 상대 경로로 저장
      this.localSource = path.directory.path
    }

    return ImportedSource(sourceId, baseDirectory, parsed.left().get())
  }
}

data class ImportedSource(
  val sourceId: BibixIdProto.SourceId,
  val baseDirectory: File,
  val buildScript: BibixAst.BuildScript,
)
