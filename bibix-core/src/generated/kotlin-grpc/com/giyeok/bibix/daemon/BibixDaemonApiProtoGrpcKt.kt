package com.giyeok.bibix.daemon

import com.giyeok.bibix.daemon.BibixDaemonApiGrpc.getServiceDescriptor
import io.grpc.CallOptions
import io.grpc.CallOptions.DEFAULT
import io.grpc.Channel
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.ServerServiceDefinition
import io.grpc.ServerServiceDefinition.builder
import io.grpc.ServiceDescriptor
import io.grpc.Status
import io.grpc.Status.UNIMPLEMENTED
import io.grpc.StatusException
import io.grpc.kotlin.AbstractCoroutineServerImpl
import io.grpc.kotlin.AbstractCoroutineStub
import io.grpc.kotlin.ClientCalls
import io.grpc.kotlin.ClientCalls.serverStreamingRpc
import io.grpc.kotlin.ClientCalls.unaryRpc
import io.grpc.kotlin.ServerCalls
import io.grpc.kotlin.ServerCalls.serverStreamingServerMethodDefinition
import io.grpc.kotlin.ServerCalls.unaryServerMethodDefinition
import io.grpc.kotlin.StubFor
import kotlin.String
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlinx.coroutines.flow.Flow

/**
 * Holder for Kotlin coroutine-based client and server APIs for
 * com.giyeok.bibix.daemon.BibixDaemonApi.
 */
object BibixDaemonApiGrpcKt {
  const val SERVICE_NAME: String = BibixDaemonApiGrpc.SERVICE_NAME

  @JvmStatic
  val serviceDescriptor: ServiceDescriptor
    get() = BibixDaemonApiGrpc.getServiceDescriptor()

  val getRepoInfoMethod: MethodDescriptor<BibixDaemonApiProto.GetRepoInfoReq,
      BibixDaemonApiProto.RepoInfo>
    @JvmStatic
    get() = BibixDaemonApiGrpc.getGetRepoInfoMethod()

  val reloadScriptMethod: MethodDescriptor<BibixDaemonApiProto.ReloadScriptReq,
      BibixDaemonApiProto.RepoInfo>
    @JvmStatic
    get() = BibixDaemonApiGrpc.getReloadScriptMethod()

  val buildTargetMethod: MethodDescriptor<BibixDaemonApiProto.BuildTargetReq,
      BibixDaemonApiProto.BuiltTargetInfo>
    @JvmStatic
    get() = BibixDaemonApiGrpc.getBuildTargetMethod()

  val invokeActionMethod: MethodDescriptor<BibixDaemonApiProto.InvokeActionReq,
      BibixDaemonApiProto.ActionResult>
    @JvmStatic
    get() = BibixDaemonApiGrpc.getInvokeActionMethod()

  val streamingInvokeActionMethod: MethodDescriptor<BibixDaemonApiProto.InvokeActionReq,
      BibixDaemonApiProto.StreamingActionEvent>
    @JvmStatic
    get() = BibixDaemonApiGrpc.getStreamingInvokeActionMethod()

  /**
   * A stub for issuing RPCs to a(n) com.giyeok.bibix.daemon.BibixDaemonApi service as suspending
   * coroutines.
   */
  @StubFor(BibixDaemonApiGrpc::class)
  class BibixDaemonApiCoroutineStub @JvmOverloads constructor(
    channel: Channel,
    callOptions: CallOptions = DEFAULT
  ) : AbstractCoroutineStub<BibixDaemonApiCoroutineStub>(channel, callOptions) {
    override fun build(channel: Channel, callOptions: CallOptions): BibixDaemonApiCoroutineStub =
        BibixDaemonApiCoroutineStub(channel, callOptions)

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][Status].  If the RPC completes with another status, a corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    suspend fun getRepoInfo(request: BibixDaemonApiProto.GetRepoInfoReq, headers: Metadata =
        Metadata()): BibixDaemonApiProto.RepoInfo = unaryRpc(
      channel,
      BibixDaemonApiGrpc.getGetRepoInfoMethod(),
      request,
      callOptions,
      headers
    )
    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][Status].  If the RPC completes with another status, a corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    suspend fun reloadScript(request: BibixDaemonApiProto.ReloadScriptReq, headers: Metadata =
        Metadata()): BibixDaemonApiProto.RepoInfo = unaryRpc(
      channel,
      BibixDaemonApiGrpc.getReloadScriptMethod(),
      request,
      callOptions,
      headers
    )
    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][Status].  If the RPC completes with another status, a corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    suspend fun buildTarget(request: BibixDaemonApiProto.BuildTargetReq, headers: Metadata =
        Metadata()): BibixDaemonApiProto.BuiltTargetInfo = unaryRpc(
      channel,
      BibixDaemonApiGrpc.getBuildTargetMethod(),
      request,
      callOptions,
      headers
    )
    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][Status].  If the RPC completes with another status, a corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    suspend fun invokeAction(request: BibixDaemonApiProto.InvokeActionReq, headers: Metadata =
        Metadata()): BibixDaemonApiProto.ActionResult = unaryRpc(
      channel,
      BibixDaemonApiGrpc.getInvokeActionMethod(),
      request,
      callOptions,
      headers
    )
    /**
     * Returns a [Flow] that, when collected, executes this RPC and emits responses from the
     * server as they arrive.  That flow finishes normally if the server closes its response with
     * [`Status.OK`][Status], and fails by throwing a [StatusException] otherwise.  If
     * collecting the flow downstream fails exceptionally (including via cancellation), the RPC
     * is cancelled with that exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return A flow that, when collected, emits the responses from the server.
     */
    fun streamingInvokeAction(request: BibixDaemonApiProto.InvokeActionReq, headers: Metadata =
        Metadata()): Flow<BibixDaemonApiProto.StreamingActionEvent> = serverStreamingRpc(
      channel,
      BibixDaemonApiGrpc.getStreamingInvokeActionMethod(),
      request,
      callOptions,
      headers
    )}

  /**
   * Skeletal implementation of the com.giyeok.bibix.daemon.BibixDaemonApi service based on Kotlin
   * coroutines.
   */
  abstract class BibixDaemonApiCoroutineImplBase(
    coroutineContext: CoroutineContext = EmptyCoroutineContext
  ) : AbstractCoroutineServerImpl(coroutineContext) {
    /**
     * Returns the response to an RPC for com.giyeok.bibix.daemon.BibixDaemonApi.GetRepoInfo.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC
     * will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    open suspend fun getRepoInfo(request: BibixDaemonApiProto.GetRepoInfoReq):
        BibixDaemonApiProto.RepoInfo = throw
        StatusException(UNIMPLEMENTED.withDescription("Method com.giyeok.bibix.daemon.BibixDaemonApi.GetRepoInfo is unimplemented"))

    /**
     * Returns the response to an RPC for com.giyeok.bibix.daemon.BibixDaemonApi.ReloadScript.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC
     * will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    open suspend fun reloadScript(request: BibixDaemonApiProto.ReloadScriptReq):
        BibixDaemonApiProto.RepoInfo = throw
        StatusException(UNIMPLEMENTED.withDescription("Method com.giyeok.bibix.daemon.BibixDaemonApi.ReloadScript is unimplemented"))

    /**
     * Returns the response to an RPC for com.giyeok.bibix.daemon.BibixDaemonApi.BuildTarget.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC
     * will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    open suspend fun buildTarget(request: BibixDaemonApiProto.BuildTargetReq):
        BibixDaemonApiProto.BuiltTargetInfo = throw
        StatusException(UNIMPLEMENTED.withDescription("Method com.giyeok.bibix.daemon.BibixDaemonApi.BuildTarget is unimplemented"))

    /**
     * Returns the response to an RPC for com.giyeok.bibix.daemon.BibixDaemonApi.InvokeAction.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC
     * will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    open suspend fun invokeAction(request: BibixDaemonApiProto.InvokeActionReq):
        BibixDaemonApiProto.ActionResult = throw
        StatusException(UNIMPLEMENTED.withDescription("Method com.giyeok.bibix.daemon.BibixDaemonApi.InvokeAction is unimplemented"))

    /**
     * Returns a [Flow] of responses to an RPC for
     * com.giyeok.bibix.daemon.BibixDaemonApi.StreamingInvokeAction.
     *
     * If creating or collecting the returned flow fails with a [StatusException], the RPC
     * will fail with the corresponding [Status].  If it fails with a
     * [java.util.concurrent.CancellationException], the RPC will fail with status
     * `Status.CANCELLED`.  If creating
     * or collecting the returned flow fails for any other reason, the RPC will fail with
     * `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    open fun streamingInvokeAction(request: BibixDaemonApiProto.InvokeActionReq):
        Flow<BibixDaemonApiProto.StreamingActionEvent> = throw
        StatusException(UNIMPLEMENTED.withDescription("Method com.giyeok.bibix.daemon.BibixDaemonApi.StreamingInvokeAction is unimplemented"))

    final override fun bindService(): ServerServiceDefinition = builder(getServiceDescriptor())
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = BibixDaemonApiGrpc.getGetRepoInfoMethod(),
      implementation = ::getRepoInfo
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = BibixDaemonApiGrpc.getReloadScriptMethod(),
      implementation = ::reloadScript
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = BibixDaemonApiGrpc.getBuildTargetMethod(),
      implementation = ::buildTarget
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = BibixDaemonApiGrpc.getInvokeActionMethod(),
      implementation = ::invokeAction
    ))
      .addMethod(serverStreamingServerMethodDefinition(
      context = this.context,
      descriptor = BibixDaemonApiGrpc.getStreamingInvokeActionMethod(),
      implementation = ::streamingInvokeAction
    )).build()
  }
}
