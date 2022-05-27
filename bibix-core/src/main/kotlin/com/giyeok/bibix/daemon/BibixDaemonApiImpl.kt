package com.giyeok.bibix.daemon

import kotlinx.coroutines.flow.Flow
import com.giyeok.bibix.daemon.BibixDaemonApiProto.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

class BibixDaemonApiImpl : BibixDaemonApiGrpcKt.BibixDaemonApiCoroutineImplBase() {
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
}
