package com.giyeok.bibix.intellij.service

import com.giyeok.bibix.graph.BibixProjectLocation
import com.giyeok.bibix.intellij.BibixIntellijProto.*
import com.giyeok.bibix.intellij.BibixIntellijServiceGrpcKt.BibixIntellijServiceCoroutineImplBase
import com.giyeok.bibix.repo.sha1Hash
import com.google.common.flogger.FluentLogger
import com.google.protobuf.ByteString
import com.google.protobuf.kotlin.toByteStringUtf8
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.io.path.absolute
import kotlin.io.path.readBytes

class BibixIntellijServiceImpl(
  private val fileSystem: FileSystem = FileSystems.getDefault(),
  private val workers: ExecutorService = Executors.newFixedThreadPool(4),
): BibixIntellijServiceCoroutineImplBase() {
  val logger = FluentLogger.forEnclosingClass()

  data class ProjectInfoMemo(
    val scriptHash: ByteString,
    val time: Instant,
    val info: StateFlow<BibixProjectInfo?>
  )

  private val mutex = Mutex()
  private val memos = mutableMapOf<BibixProjectLocation, ProjectInfoMemo>()

  private suspend fun startLoadScript(
    key: BibixProjectLocation,
    scriptHash: ByteString
  ): StateFlow<BibixProjectInfo?> {
    val (projectRoot, scriptName) = key

    val flow = MutableStateFlow<BibixProjectInfo?>(null)
    memos[key] = ProjectInfoMemo(scriptHash, Instant.now(), flow)
    workers.execute {
      val loaded = try {
        ProjectStructureExtractor(BibixProjectLocation.of(projectRoot, scriptName)).loadProject()
      } catch (e: Exception) {
        e.printStackTrace()
        throw StatusException(Status.FAILED_PRECONDITION)
      }

      runBlocking {
        flow.emit(loaded)
      }
    }
    return flow
  }

  override suspend fun loadProject(request: LoadProjectReq): BibixProjectInfo {
    logger.atFine().log("loadProject: $request")
    println("${LocalDateTime.now()} loadProject: $request")

    val projectRoot = fileSystem.getPath(request.projectRoot).normalize().absolute()
    val scriptName = request.scriptName.ifEmpty { null }
    val projectLocation = BibixProjectLocation.of(projectRoot, scriptName)

    val scriptHash = sha1Hash(projectLocation.readScript().toByteStringUtf8())

    val flow = mutex.withLock {
      if (request.forceReload) {
        startLoadScript(projectLocation, scriptHash)
      } else {
        val existing = memos[projectLocation]
        if (existing != null && existing.scriptHash == scriptHash &&
          Duration.between(existing.time, Instant.now()) < Duration.ofMinutes(1)
        ) {
          existing.info
        } else {
          startLoadScript(projectLocation, scriptHash)
        }
      }
    }

    return flow.filterNotNull().first()
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
