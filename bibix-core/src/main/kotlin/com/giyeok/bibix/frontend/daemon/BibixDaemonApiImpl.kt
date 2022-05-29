package com.giyeok.bibix.frontend.daemon

import com.giyeok.bibix.daemon.BibixDaemonApiGrpcKt
import com.giyeok.bibix.daemon.BibixDaemonApiProto.*
import com.giyeok.bibix.daemon.streamingActionEvent
import com.giyeok.bibix.frontend.BuildFrontend
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

class BibixDaemonApiImpl(val projectDir: File) :
  BibixDaemonApiGrpcKt.BibixDaemonApiCoroutineImplBase() {

  lateinit var frontend: BuildFrontend

  init {
    frontend = BuildFrontend(projectDir)
  }

  override suspend fun getRepoInfo(request: GetRepoInfoReq): RepoInfo {
    return super.getRepoInfo(request)
  }

  override suspend fun reloadScript(request: ReloadScriptReq): RepoInfo {
    return super.reloadScript(request)
  }

  override suspend fun buildTarget(request: BuildTargetReq): BuiltTargetInfo {
    return super.buildTarget(request)
  }

  override suspend fun invokeAction(request: InvokeActionReq): ActionResult {
    return super.invokeAction(request)
  }

  override fun streamingInvokeAction(request: InvokeActionReq): Flow<StreamingActionEvent> {
    return flow {
      emit(streamingActionEvent {
        this.stdout = "new message"
      })
      delay(500)
      emit(streamingActionEvent {
        this.stdout = "done"
      })
    }
  }

  override suspend fun getIntellijProjectStructure(request: GetIntellijProjectStructureReq): IntellijProjectStructure {
    val frontend = this.frontend
    if (frontend.parseError != null) {
      throw StatusException(Status.ABORTED.withDescription(frontend.parseError.msg()))
    }
    return IntellijProjectExtractor(frontend).extractIntellijProjectStructure()
  }
}
