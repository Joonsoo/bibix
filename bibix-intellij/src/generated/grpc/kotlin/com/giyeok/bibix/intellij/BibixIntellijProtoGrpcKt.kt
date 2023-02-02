package com.giyeok.bibix.intellij

import com.giyeok.bibix.intellij.BibixIntellijServiceGrpc.getServiceDescriptor
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
 * com.giyeok.bibix.intellij.BibixIntellijService.
 */
object BibixIntellijServiceGrpcKt {
  const val SERVICE_NAME: String = BibixIntellijServiceGrpc.SERVICE_NAME

  @JvmStatic
  val serviceDescriptor: ServiceDescriptor
    get() = BibixIntellijServiceGrpc.getServiceDescriptor()

  val loadProjectMethod: MethodDescriptor<BibixIntellijProto.LoadProjectReq,
      BibixIntellijProto.BibixProjectInfo>
    @JvmStatic
    get() = BibixIntellijServiceGrpc.getLoadProjectMethod()

  val buildTargetsMethod: MethodDescriptor<BibixIntellijProto.BuildTargetsReq,
      BibixIntellijProto.BuildTargetsRes>
    @JvmStatic
    get() = BibixIntellijServiceGrpc.getBuildTargetsMethod()

  val buildTargetsStreamingMethod: MethodDescriptor<BibixIntellijProto.BuildTargetsReq,
      BibixIntellijProto.BuildTargetsUpdate>
    @JvmStatic
    get() = BibixIntellijServiceGrpc.getBuildTargetsStreamingMethod()

  val executeActionsMethod: MethodDescriptor<BibixIntellijProto.ExecuteActionsReq,
      BibixIntellijProto.ExecuteActionsRes>
    @JvmStatic
    get() = BibixIntellijServiceGrpc.getExecuteActionsMethod()

  val executeActionsStreamingMethod: MethodDescriptor<BibixIntellijProto.BuildTargetsReq,
      BibixIntellijProto.ExecuteActionUpdate>
    @JvmStatic
    get() = BibixIntellijServiceGrpc.getExecuteActionsStreamingMethod()

  /**
   * A stub for issuing RPCs to a(n) com.giyeok.bibix.intellij.BibixIntellijService service as
   * suspending coroutines.
   */
  @StubFor(BibixIntellijServiceGrpc::class)
  class BibixIntellijServiceCoroutineStub @JvmOverloads constructor(
    channel: Channel,
    callOptions: CallOptions = DEFAULT
  ) : AbstractCoroutineStub<BibixIntellijServiceCoroutineStub>(channel, callOptions) {
    override fun build(channel: Channel, callOptions: CallOptions):
        BibixIntellijServiceCoroutineStub = BibixIntellijServiceCoroutineStub(channel, callOptions)

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
    suspend fun loadProject(request: BibixIntellijProto.LoadProjectReq, headers: Metadata =
        Metadata()): BibixIntellijProto.BibixProjectInfo = unaryRpc(
      channel,
      BibixIntellijServiceGrpc.getLoadProjectMethod(),
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
    suspend fun buildTargets(request: BibixIntellijProto.BuildTargetsReq, headers: Metadata =
        Metadata()): BibixIntellijProto.BuildTargetsRes = unaryRpc(
      channel,
      BibixIntellijServiceGrpc.getBuildTargetsMethod(),
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
    fun buildTargetsStreaming(request: BibixIntellijProto.BuildTargetsReq, headers: Metadata =
        Metadata()): Flow<BibixIntellijProto.BuildTargetsUpdate> = serverStreamingRpc(
      channel,
      BibixIntellijServiceGrpc.getBuildTargetsStreamingMethod(),
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
    suspend fun executeActions(request: BibixIntellijProto.ExecuteActionsReq, headers: Metadata =
        Metadata()): BibixIntellijProto.ExecuteActionsRes = unaryRpc(
      channel,
      BibixIntellijServiceGrpc.getExecuteActionsMethod(),
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
    fun executeActionsStreaming(request: BibixIntellijProto.BuildTargetsReq, headers: Metadata =
        Metadata()): Flow<BibixIntellijProto.ExecuteActionUpdate> = serverStreamingRpc(
      channel,
      BibixIntellijServiceGrpc.getExecuteActionsStreamingMethod(),
      request,
      callOptions,
      headers
    )}

  /**
   * Skeletal implementation of the com.giyeok.bibix.intellij.BibixIntellijService service based on
   * Kotlin coroutines.
   */
  abstract class BibixIntellijServiceCoroutineImplBase(
    coroutineContext: CoroutineContext = EmptyCoroutineContext
  ) : AbstractCoroutineServerImpl(coroutineContext) {
    /**
     * Returns the response to an RPC for
     * com.giyeok.bibix.intellij.BibixIntellijService.loadProject.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC
     * will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    open suspend fun loadProject(request: BibixIntellijProto.LoadProjectReq):
        BibixIntellijProto.BibixProjectInfo = throw
        StatusException(UNIMPLEMENTED.withDescription("Method com.giyeok.bibix.intellij.BibixIntellijService.loadProject is unimplemented"))

    /**
     * Returns the response to an RPC for
     * com.giyeok.bibix.intellij.BibixIntellijService.buildTargets.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC
     * will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    open suspend fun buildTargets(request: BibixIntellijProto.BuildTargetsReq):
        BibixIntellijProto.BuildTargetsRes = throw
        StatusException(UNIMPLEMENTED.withDescription("Method com.giyeok.bibix.intellij.BibixIntellijService.buildTargets is unimplemented"))

    /**
     * Returns a [Flow] of responses to an RPC for
     * com.giyeok.bibix.intellij.BibixIntellijService.buildTargetsStreaming.
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
    open fun buildTargetsStreaming(request: BibixIntellijProto.BuildTargetsReq):
        Flow<BibixIntellijProto.BuildTargetsUpdate> = throw
        StatusException(UNIMPLEMENTED.withDescription("Method com.giyeok.bibix.intellij.BibixIntellijService.buildTargetsStreaming is unimplemented"))

    /**
     * Returns the response to an RPC for
     * com.giyeok.bibix.intellij.BibixIntellijService.executeActions.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC
     * will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    open suspend fun executeActions(request: BibixIntellijProto.ExecuteActionsReq):
        BibixIntellijProto.ExecuteActionsRes = throw
        StatusException(UNIMPLEMENTED.withDescription("Method com.giyeok.bibix.intellij.BibixIntellijService.executeActions is unimplemented"))

    /**
     * Returns a [Flow] of responses to an RPC for
     * com.giyeok.bibix.intellij.BibixIntellijService.executeActionsStreaming.
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
    open fun executeActionsStreaming(request: BibixIntellijProto.BuildTargetsReq):
        Flow<BibixIntellijProto.ExecuteActionUpdate> = throw
        StatusException(UNIMPLEMENTED.withDescription("Method com.giyeok.bibix.intellij.BibixIntellijService.executeActionsStreaming is unimplemented"))

    final override fun bindService(): ServerServiceDefinition = builder(getServiceDescriptor())
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = BibixIntellijServiceGrpc.getLoadProjectMethod(),
      implementation = ::loadProject
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = BibixIntellijServiceGrpc.getBuildTargetsMethod(),
      implementation = ::buildTargets
    ))
      .addMethod(serverStreamingServerMethodDefinition(
      context = this.context,
      descriptor = BibixIntellijServiceGrpc.getBuildTargetsStreamingMethod(),
      implementation = ::buildTargetsStreaming
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = BibixIntellijServiceGrpc.getExecuteActionsMethod(),
      implementation = ::executeActions
    ))
      .addMethod(serverStreamingServerMethodDefinition(
      context = this.context,
      descriptor = BibixIntellijServiceGrpc.getExecuteActionsStreamingMethod(),
      implementation = ::executeActionsStreaming
    )).build()
  }
}
