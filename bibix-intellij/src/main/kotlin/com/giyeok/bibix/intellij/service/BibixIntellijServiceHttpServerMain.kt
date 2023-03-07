package com.giyeok.bibix.intellij.service

import com.giyeok.bibix.intellij.BibixIntellijProto.LoadProjectReq
import com.google.common.flogger.FluentLogger
import com.google.protobuf.util.JsonFormat
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress

object BibixIntellijServiceHttpServerMain {
  val logger = FluentLogger.forEnclosingClass()

  @JvmStatic
  fun main(args: Array<String>) {
    val portNum = 8089

    val service = BibixIntellijServiceImpl()

    val server = HttpServer.create(InetSocketAddress(portNum), 0)

    server.createContext("/loadProject") { exchange ->
      val requestBuilder = LoadProjectReq.newBuilder()
      JsonFormat.parser().merge(exchange.requestBody.readAllBytes().toString(), requestBuilder)
      val request = requestBuilder.build()

      val response = runBlocking { service.loadProject(request) }

      exchange.responseBody.write(response.toByteArray())
      exchange.responseBody.close()
    }
    logger.atInfo().log("Starting to listen HTTP at port $portNum")
    server.start()
  }
}
