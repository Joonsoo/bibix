package com.giyeok.bibix.frontend.daemon

import com.google.common.flogger.FluentLogger
import io.grpc.*
import java.util.logging.Level

class ReceivedRequestLogInterceptor(val level: Level = Level.FINEST) : ServerInterceptor {
  private val logger: FluentLogger = FluentLogger.forEnclosingClass()

  override fun <ReqT : Any, RespT : Any> interceptCall(
    call: ServerCall<ReqT, RespT>,
    headers: io.grpc.Metadata,
    next: ServerCallHandler<ReqT, RespT>
  ): ServerCall.Listener<ReqT> {
    val listener: ServerCall<ReqT, RespT> = object : ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
    }
    return object : ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(next.startCall(listener, headers)) {
      override fun onMessage(message: ReqT) {
        logger.at(level)
          .log("Received message from client: %s, %s", message::class.simpleName, message)
        super.onMessage(message)
      }
    }
  }
}
