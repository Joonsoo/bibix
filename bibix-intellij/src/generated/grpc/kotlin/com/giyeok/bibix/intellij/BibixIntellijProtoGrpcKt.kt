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
import io.grpc.Status.UNIMPLEMENTED
import io.grpc.StatusException
import io.grpc.kotlin.AbstractCoroutineServerImpl
import io.grpc.kotlin.AbstractCoroutineStub
import io.grpc.kotlin.ClientCalls.unaryRpc
import io.grpc.kotlin.ServerCalls.unaryServerMethodDefinition
import io.grpc.kotlin.StubFor
import kotlin.String
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

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
     * with [`Status.OK`][io.grpc.Status].  If the RPC completes with another status, a
     * corresponding
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
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException],
     * the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    open suspend fun loadProject(request: BibixIntellijProto.LoadProjectReq):
        BibixIntellijProto.BibixProjectInfo = throw
        StatusException(UNIMPLEMENTED.withDescription("Method com.giyeok.bibix.intellij.BibixIntellijService.loadProject is unimplemented"))

    final override fun bindService(): ServerServiceDefinition = builder(getServiceDescriptor())
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = BibixIntellijServiceGrpc.getLoadProjectMethod(),
      implementation = ::loadProject
    )).build()
  }
}
