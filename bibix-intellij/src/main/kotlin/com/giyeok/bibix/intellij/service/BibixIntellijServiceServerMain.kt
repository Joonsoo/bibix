package com.giyeok.bibix.intellij.service

import com.google.common.flogger.FluentLogger
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import java.util.concurrent.Executors

object BibixIntellijServiceServerMain {
  val logger = FluentLogger.forEnclosingClass()

  @JvmStatic
  fun main(args: Array<String>) {
    val portNum = 8088
    val server = NettyServerBuilder.forPort(portNum)
      .addService(BibixIntellijServiceImpl())
      .executor(Executors.newFixedThreadPool(32))
      .build()
    logger.atInfo().log("Starting to listen GRPC at port $portNum")
    server.start()

    try {
      server.awaitTermination()
    } finally {
      server.shutdown()
    }
  }
}
