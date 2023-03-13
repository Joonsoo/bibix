package com.giyeok.bibix.intellij.service

import com.giyeok.bibix.intellij.BibixIntellijProto.*
import com.giyeok.bibix.intellij.BibixIntellijServiceGrpcKt.BibixIntellijServiceCoroutineImplBase
import com.giyeok.bibix.repo.sha1Hash
import com.google.common.flogger.FluentLogger
import com.google.protobuf.ByteString
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.flow.Flow
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDateTime
import kotlin.io.path.absolute
import kotlin.io.path.readBytes

class BibixIntellijServiceImpl(
  private val fileSystem: FileSystem = FileSystems.getDefault(),
) : BibixIntellijServiceCoroutineImplBase() {
  val logger = FluentLogger.forEnclosingClass()

  private val memos = mutableMapOf<Pair<Path, String?>, Pair<ByteString, BibixProjectInfo>>()

  override suspend fun loadProject(request: LoadProjectReq): BibixProjectInfo {
    logger.atFine().log("loadProject: $request")
    println("${LocalDateTime.now()} loadProject: $request")

    val projectRoot = fileSystem.getPath(request.projectRoot).normalize().absolute()
    val scriptName = request.scriptName.ifEmpty { null }
    val key = Pair(projectRoot, scriptName)

    val scriptPath = projectRoot.resolve(scriptName ?: "build.bbx")
    val scriptHash = sha1Hash(scriptPath.readBytes())

    if (!request.forceReload) {
      val existing = memos[key]
      if (existing != null && existing.first == scriptHash) {
        return existing.second
      }
    }

    val loaded = try {
      ProjectStructureExtractor.loadProject(projectRoot, scriptName)
    } catch (e: Exception) {
      e.printStackTrace()
      throw StatusException(Status.FAILED_PRECONDITION)
    }
    memos[key] = Pair(scriptHash, loaded)

    println("Done")

    return loaded
  }

  override suspend fun buildTargets(request: BuildTargetsReq): BuildTargetsRes {
    return super.buildTargets(request)
  }

  override fun buildTargetsStreaming(request: BuildTargetsReq): Flow<BuildTargetsUpdate> {
    return super.buildTargetsStreaming(request)
  }

  override suspend fun executeActions(request: ExecuteActionsReq): ExecuteActionsRes {
    return super.executeActions(request)
  }

  override fun executeActionsStreaming(request: BuildTargetsReq): Flow<ExecuteActionUpdate> {
    return super.executeActionsStreaming(request)
  }
}
