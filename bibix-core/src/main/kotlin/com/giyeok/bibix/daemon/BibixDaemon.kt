package com.giyeok.bibix.daemon

import com.google.common.flogger.FluentLogger
import io.grpc.Server
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import java.util.concurrent.Executors

class BibixDaemon {
  private val logger = FluentLogger.forEnclosingClass()

  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      val server = BibixDaemon().startServer()

      try {
        server.awaitTermination()
      } finally {
        server.shutdown()
      }
    }
  }

  fun startServer(): Server {
    val grpcPort = 61617

    val server = NettyServerBuilder.forPort(grpcPort)
      .addService(BibixDaemonApiImpl())
//      .intercept(UnexpectedExceptionsInterceptor())
//      .intercept(ReceivedRequestLogInterceptor(Level.INFO))
      .executor(Executors.newFixedThreadPool(4))
      .build()
    logger.atInfo().log("Starting to listen GRPC at port $grpcPort")
    server.start()
    return server
  }
}
