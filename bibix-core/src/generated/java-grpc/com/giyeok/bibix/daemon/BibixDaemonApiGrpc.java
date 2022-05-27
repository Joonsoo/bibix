package com.giyeok.bibix.daemon;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.46.0)",
    comments = "Source: api.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class BibixDaemonApiGrpc {

  private BibixDaemonApiGrpc() {}

  public static final String SERVICE_NAME = "com.giyeok.bibix.daemon.BibixDaemonApi";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.giyeok.bibix.daemon.BibixDaemonApiProto.GetRepoInfoReq,
      com.giyeok.bibix.daemon.BibixDaemonApiProto.RepoInfo> getGetRepoInfoMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetRepoInfo",
      requestType = com.giyeok.bibix.daemon.BibixDaemonApiProto.GetRepoInfoReq.class,
      responseType = com.giyeok.bibix.daemon.BibixDaemonApiProto.RepoInfo.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.giyeok.bibix.daemon.BibixDaemonApiProto.GetRepoInfoReq,
      com.giyeok.bibix.daemon.BibixDaemonApiProto.RepoInfo> getGetRepoInfoMethod() {
    io.grpc.MethodDescriptor<com.giyeok.bibix.daemon.BibixDaemonApiProto.GetRepoInfoReq, com.giyeok.bibix.daemon.BibixDaemonApiProto.RepoInfo> getGetRepoInfoMethod;
    if ((getGetRepoInfoMethod = BibixDaemonApiGrpc.getGetRepoInfoMethod) == null) {
      synchronized (BibixDaemonApiGrpc.class) {
        if ((getGetRepoInfoMethod = BibixDaemonApiGrpc.getGetRepoInfoMethod) == null) {
          BibixDaemonApiGrpc.getGetRepoInfoMethod = getGetRepoInfoMethod =
              io.grpc.MethodDescriptor.<com.giyeok.bibix.daemon.BibixDaemonApiProto.GetRepoInfoReq, com.giyeok.bibix.daemon.BibixDaemonApiProto.RepoInfo>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetRepoInfo"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.giyeok.bibix.daemon.BibixDaemonApiProto.GetRepoInfoReq.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.giyeok.bibix.daemon.BibixDaemonApiProto.RepoInfo.getDefaultInstance()))
              .setSchemaDescriptor(new BibixDaemonApiMethodDescriptorSupplier("GetRepoInfo"))
              .build();
        }
      }
    }
    return getGetRepoInfoMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.giyeok.bibix.daemon.BibixDaemonApiProto.ReloadScriptReq,
      com.giyeok.bibix.daemon.BibixDaemonApiProto.RepoInfo> getReloadScriptMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ReloadScript",
      requestType = com.giyeok.bibix.daemon.BibixDaemonApiProto.ReloadScriptReq.class,
      responseType = com.giyeok.bibix.daemon.BibixDaemonApiProto.RepoInfo.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.giyeok.bibix.daemon.BibixDaemonApiProto.ReloadScriptReq,
      com.giyeok.bibix.daemon.BibixDaemonApiProto.RepoInfo> getReloadScriptMethod() {
    io.grpc.MethodDescriptor<com.giyeok.bibix.daemon.BibixDaemonApiProto.ReloadScriptReq, com.giyeok.bibix.daemon.BibixDaemonApiProto.RepoInfo> getReloadScriptMethod;
    if ((getReloadScriptMethod = BibixDaemonApiGrpc.getReloadScriptMethod) == null) {
      synchronized (BibixDaemonApiGrpc.class) {
        if ((getReloadScriptMethod = BibixDaemonApiGrpc.getReloadScriptMethod) == null) {
          BibixDaemonApiGrpc.getReloadScriptMethod = getReloadScriptMethod =
              io.grpc.MethodDescriptor.<com.giyeok.bibix.daemon.BibixDaemonApiProto.ReloadScriptReq, com.giyeok.bibix.daemon.BibixDaemonApiProto.RepoInfo>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ReloadScript"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.giyeok.bibix.daemon.BibixDaemonApiProto.ReloadScriptReq.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.giyeok.bibix.daemon.BibixDaemonApiProto.RepoInfo.getDefaultInstance()))
              .setSchemaDescriptor(new BibixDaemonApiMethodDescriptorSupplier("ReloadScript"))
              .build();
        }
      }
    }
    return getReloadScriptMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.giyeok.bibix.daemon.BibixDaemonApiProto.BuildTargetReq,
      com.giyeok.bibix.daemon.BibixDaemonApiProto.BuiltTargetInfo> getBuildTargetMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "BuildTarget",
      requestType = com.giyeok.bibix.daemon.BibixDaemonApiProto.BuildTargetReq.class,
      responseType = com.giyeok.bibix.daemon.BibixDaemonApiProto.BuiltTargetInfo.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.giyeok.bibix.daemon.BibixDaemonApiProto.BuildTargetReq,
      com.giyeok.bibix.daemon.BibixDaemonApiProto.BuiltTargetInfo> getBuildTargetMethod() {
    io.grpc.MethodDescriptor<com.giyeok.bibix.daemon.BibixDaemonApiProto.BuildTargetReq, com.giyeok.bibix.daemon.BibixDaemonApiProto.BuiltTargetInfo> getBuildTargetMethod;
    if ((getBuildTargetMethod = BibixDaemonApiGrpc.getBuildTargetMethod) == null) {
      synchronized (BibixDaemonApiGrpc.class) {
        if ((getBuildTargetMethod = BibixDaemonApiGrpc.getBuildTargetMethod) == null) {
          BibixDaemonApiGrpc.getBuildTargetMethod = getBuildTargetMethod =
              io.grpc.MethodDescriptor.<com.giyeok.bibix.daemon.BibixDaemonApiProto.BuildTargetReq, com.giyeok.bibix.daemon.BibixDaemonApiProto.BuiltTargetInfo>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "BuildTarget"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.giyeok.bibix.daemon.BibixDaemonApiProto.BuildTargetReq.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.giyeok.bibix.daemon.BibixDaemonApiProto.BuiltTargetInfo.getDefaultInstance()))
              .setSchemaDescriptor(new BibixDaemonApiMethodDescriptorSupplier("BuildTarget"))
              .build();
        }
      }
    }
    return getBuildTargetMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.giyeok.bibix.daemon.BibixDaemonApiProto.InvokeActionReq,
      com.giyeok.bibix.daemon.BibixDaemonApiProto.ActionResult> getInvokeActionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "InvokeAction",
      requestType = com.giyeok.bibix.daemon.BibixDaemonApiProto.InvokeActionReq.class,
      responseType = com.giyeok.bibix.daemon.BibixDaemonApiProto.ActionResult.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.giyeok.bibix.daemon.BibixDaemonApiProto.InvokeActionReq,
      com.giyeok.bibix.daemon.BibixDaemonApiProto.ActionResult> getInvokeActionMethod() {
    io.grpc.MethodDescriptor<com.giyeok.bibix.daemon.BibixDaemonApiProto.InvokeActionReq, com.giyeok.bibix.daemon.BibixDaemonApiProto.ActionResult> getInvokeActionMethod;
    if ((getInvokeActionMethod = BibixDaemonApiGrpc.getInvokeActionMethod) == null) {
      synchronized (BibixDaemonApiGrpc.class) {
        if ((getInvokeActionMethod = BibixDaemonApiGrpc.getInvokeActionMethod) == null) {
          BibixDaemonApiGrpc.getInvokeActionMethod = getInvokeActionMethod =
              io.grpc.MethodDescriptor.<com.giyeok.bibix.daemon.BibixDaemonApiProto.InvokeActionReq, com.giyeok.bibix.daemon.BibixDaemonApiProto.ActionResult>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "InvokeAction"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.giyeok.bibix.daemon.BibixDaemonApiProto.InvokeActionReq.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.giyeok.bibix.daemon.BibixDaemonApiProto.ActionResult.getDefaultInstance()))
              .setSchemaDescriptor(new BibixDaemonApiMethodDescriptorSupplier("InvokeAction"))
              .build();
        }
      }
    }
    return getInvokeActionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.giyeok.bibix.daemon.BibixDaemonApiProto.InvokeActionReq,
      com.giyeok.bibix.daemon.BibixDaemonApiProto.StreamingActionEvent> getStreamingInvokeActionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "StreamingInvokeAction",
      requestType = com.giyeok.bibix.daemon.BibixDaemonApiProto.InvokeActionReq.class,
      responseType = com.giyeok.bibix.daemon.BibixDaemonApiProto.StreamingActionEvent.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.giyeok.bibix.daemon.BibixDaemonApiProto.InvokeActionReq,
      com.giyeok.bibix.daemon.BibixDaemonApiProto.StreamingActionEvent> getStreamingInvokeActionMethod() {
    io.grpc.MethodDescriptor<com.giyeok.bibix.daemon.BibixDaemonApiProto.InvokeActionReq, com.giyeok.bibix.daemon.BibixDaemonApiProto.StreamingActionEvent> getStreamingInvokeActionMethod;
    if ((getStreamingInvokeActionMethod = BibixDaemonApiGrpc.getStreamingInvokeActionMethod) == null) {
      synchronized (BibixDaemonApiGrpc.class) {
        if ((getStreamingInvokeActionMethod = BibixDaemonApiGrpc.getStreamingInvokeActionMethod) == null) {
          BibixDaemonApiGrpc.getStreamingInvokeActionMethod = getStreamingInvokeActionMethod =
              io.grpc.MethodDescriptor.<com.giyeok.bibix.daemon.BibixDaemonApiProto.InvokeActionReq, com.giyeok.bibix.daemon.BibixDaemonApiProto.StreamingActionEvent>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "StreamingInvokeAction"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.giyeok.bibix.daemon.BibixDaemonApiProto.InvokeActionReq.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.giyeok.bibix.daemon.BibixDaemonApiProto.StreamingActionEvent.getDefaultInstance()))
              .setSchemaDescriptor(new BibixDaemonApiMethodDescriptorSupplier("StreamingInvokeAction"))
              .build();
        }
      }
    }
    return getStreamingInvokeActionMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static BibixDaemonApiStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<BibixDaemonApiStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<BibixDaemonApiStub>() {
        @java.lang.Override
        public BibixDaemonApiStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new BibixDaemonApiStub(channel, callOptions);
        }
      };
    return BibixDaemonApiStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static BibixDaemonApiBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<BibixDaemonApiBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<BibixDaemonApiBlockingStub>() {
        @java.lang.Override
        public BibixDaemonApiBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new BibixDaemonApiBlockingStub(channel, callOptions);
        }
      };
    return BibixDaemonApiBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static BibixDaemonApiFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<BibixDaemonApiFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<BibixDaemonApiFutureStub>() {
        @java.lang.Override
        public BibixDaemonApiFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new BibixDaemonApiFutureStub(channel, callOptions);
        }
      };
    return BibixDaemonApiFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class BibixDaemonApiImplBase implements io.grpc.BindableService {

    /**
     */
    public void getRepoInfo(com.giyeok.bibix.daemon.BibixDaemonApiProto.GetRepoInfoReq request,
        io.grpc.stub.StreamObserver<com.giyeok.bibix.daemon.BibixDaemonApiProto.RepoInfo> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetRepoInfoMethod(), responseObserver);
    }

    /**
     */
    public void reloadScript(com.giyeok.bibix.daemon.BibixDaemonApiProto.ReloadScriptReq request,
        io.grpc.stub.StreamObserver<com.giyeok.bibix.daemon.BibixDaemonApiProto.RepoInfo> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getReloadScriptMethod(), responseObserver);
    }

    /**
     */
    public void buildTarget(com.giyeok.bibix.daemon.BibixDaemonApiProto.BuildTargetReq request,
        io.grpc.stub.StreamObserver<com.giyeok.bibix.daemon.BibixDaemonApiProto.BuiltTargetInfo> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getBuildTargetMethod(), responseObserver);
    }

    /**
     */
    public void invokeAction(com.giyeok.bibix.daemon.BibixDaemonApiProto.InvokeActionReq request,
        io.grpc.stub.StreamObserver<com.giyeok.bibix.daemon.BibixDaemonApiProto.ActionResult> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getInvokeActionMethod(), responseObserver);
    }

    /**
     */
    public void streamingInvokeAction(com.giyeok.bibix.daemon.BibixDaemonApiProto.InvokeActionReq request,
        io.grpc.stub.StreamObserver<com.giyeok.bibix.daemon.BibixDaemonApiProto.StreamingActionEvent> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getStreamingInvokeActionMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getGetRepoInfoMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.giyeok.bibix.daemon.BibixDaemonApiProto.GetRepoInfoReq,
                com.giyeok.bibix.daemon.BibixDaemonApiProto.RepoInfo>(
                  this, METHODID_GET_REPO_INFO)))
          .addMethod(
            getReloadScriptMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.giyeok.bibix.daemon.BibixDaemonApiProto.ReloadScriptReq,
                com.giyeok.bibix.daemon.BibixDaemonApiProto.RepoInfo>(
                  this, METHODID_RELOAD_SCRIPT)))
          .addMethod(
            getBuildTargetMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.giyeok.bibix.daemon.BibixDaemonApiProto.BuildTargetReq,
                com.giyeok.bibix.daemon.BibixDaemonApiProto.BuiltTargetInfo>(
                  this, METHODID_BUILD_TARGET)))
          .addMethod(
            getInvokeActionMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.giyeok.bibix.daemon.BibixDaemonApiProto.InvokeActionReq,
                com.giyeok.bibix.daemon.BibixDaemonApiProto.ActionResult>(
                  this, METHODID_INVOKE_ACTION)))
          .addMethod(
            getStreamingInvokeActionMethod(),
            io.grpc.stub.ServerCalls.asyncServerStreamingCall(
              new MethodHandlers<
                com.giyeok.bibix.daemon.BibixDaemonApiProto.InvokeActionReq,
                com.giyeok.bibix.daemon.BibixDaemonApiProto.StreamingActionEvent>(
                  this, METHODID_STREAMING_INVOKE_ACTION)))
          .build();
    }
  }

  /**
   */
  public static final class BibixDaemonApiStub extends io.grpc.stub.AbstractAsyncStub<BibixDaemonApiStub> {
    private BibixDaemonApiStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected BibixDaemonApiStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new BibixDaemonApiStub(channel, callOptions);
    }

    /**
     */
    public void getRepoInfo(com.giyeok.bibix.daemon.BibixDaemonApiProto.GetRepoInfoReq request,
        io.grpc.stub.StreamObserver<com.giyeok.bibix.daemon.BibixDaemonApiProto.RepoInfo> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetRepoInfoMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void reloadScript(com.giyeok.bibix.daemon.BibixDaemonApiProto.ReloadScriptReq request,
        io.grpc.stub.StreamObserver<com.giyeok.bibix.daemon.BibixDaemonApiProto.RepoInfo> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getReloadScriptMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void buildTarget(com.giyeok.bibix.daemon.BibixDaemonApiProto.BuildTargetReq request,
        io.grpc.stub.StreamObserver<com.giyeok.bibix.daemon.BibixDaemonApiProto.BuiltTargetInfo> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getBuildTargetMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void invokeAction(com.giyeok.bibix.daemon.BibixDaemonApiProto.InvokeActionReq request,
        io.grpc.stub.StreamObserver<com.giyeok.bibix.daemon.BibixDaemonApiProto.ActionResult> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getInvokeActionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void streamingInvokeAction(com.giyeok.bibix.daemon.BibixDaemonApiProto.InvokeActionReq request,
        io.grpc.stub.StreamObserver<com.giyeok.bibix.daemon.BibixDaemonApiProto.StreamingActionEvent> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getStreamingInvokeActionMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class BibixDaemonApiBlockingStub extends io.grpc.stub.AbstractBlockingStub<BibixDaemonApiBlockingStub> {
    private BibixDaemonApiBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected BibixDaemonApiBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new BibixDaemonApiBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.giyeok.bibix.daemon.BibixDaemonApiProto.RepoInfo getRepoInfo(com.giyeok.bibix.daemon.BibixDaemonApiProto.GetRepoInfoReq request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetRepoInfoMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.giyeok.bibix.daemon.BibixDaemonApiProto.RepoInfo reloadScript(com.giyeok.bibix.daemon.BibixDaemonApiProto.ReloadScriptReq request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getReloadScriptMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.giyeok.bibix.daemon.BibixDaemonApiProto.BuiltTargetInfo buildTarget(com.giyeok.bibix.daemon.BibixDaemonApiProto.BuildTargetReq request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getBuildTargetMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.giyeok.bibix.daemon.BibixDaemonApiProto.ActionResult invokeAction(com.giyeok.bibix.daemon.BibixDaemonApiProto.InvokeActionReq request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getInvokeActionMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<com.giyeok.bibix.daemon.BibixDaemonApiProto.StreamingActionEvent> streamingInvokeAction(
        com.giyeok.bibix.daemon.BibixDaemonApiProto.InvokeActionReq request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getStreamingInvokeActionMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class BibixDaemonApiFutureStub extends io.grpc.stub.AbstractFutureStub<BibixDaemonApiFutureStub> {
    private BibixDaemonApiFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected BibixDaemonApiFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new BibixDaemonApiFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.giyeok.bibix.daemon.BibixDaemonApiProto.RepoInfo> getRepoInfo(
        com.giyeok.bibix.daemon.BibixDaemonApiProto.GetRepoInfoReq request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetRepoInfoMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.giyeok.bibix.daemon.BibixDaemonApiProto.RepoInfo> reloadScript(
        com.giyeok.bibix.daemon.BibixDaemonApiProto.ReloadScriptReq request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getReloadScriptMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.giyeok.bibix.daemon.BibixDaemonApiProto.BuiltTargetInfo> buildTarget(
        com.giyeok.bibix.daemon.BibixDaemonApiProto.BuildTargetReq request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getBuildTargetMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.giyeok.bibix.daemon.BibixDaemonApiProto.ActionResult> invokeAction(
        com.giyeok.bibix.daemon.BibixDaemonApiProto.InvokeActionReq request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getInvokeActionMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_REPO_INFO = 0;
  private static final int METHODID_RELOAD_SCRIPT = 1;
  private static final int METHODID_BUILD_TARGET = 2;
  private static final int METHODID_INVOKE_ACTION = 3;
  private static final int METHODID_STREAMING_INVOKE_ACTION = 4;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final BibixDaemonApiImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(BibixDaemonApiImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_GET_REPO_INFO:
          serviceImpl.getRepoInfo((com.giyeok.bibix.daemon.BibixDaemonApiProto.GetRepoInfoReq) request,
              (io.grpc.stub.StreamObserver<com.giyeok.bibix.daemon.BibixDaemonApiProto.RepoInfo>) responseObserver);
          break;
        case METHODID_RELOAD_SCRIPT:
          serviceImpl.reloadScript((com.giyeok.bibix.daemon.BibixDaemonApiProto.ReloadScriptReq) request,
              (io.grpc.stub.StreamObserver<com.giyeok.bibix.daemon.BibixDaemonApiProto.RepoInfo>) responseObserver);
          break;
        case METHODID_BUILD_TARGET:
          serviceImpl.buildTarget((com.giyeok.bibix.daemon.BibixDaemonApiProto.BuildTargetReq) request,
              (io.grpc.stub.StreamObserver<com.giyeok.bibix.daemon.BibixDaemonApiProto.BuiltTargetInfo>) responseObserver);
          break;
        case METHODID_INVOKE_ACTION:
          serviceImpl.invokeAction((com.giyeok.bibix.daemon.BibixDaemonApiProto.InvokeActionReq) request,
              (io.grpc.stub.StreamObserver<com.giyeok.bibix.daemon.BibixDaemonApiProto.ActionResult>) responseObserver);
          break;
        case METHODID_STREAMING_INVOKE_ACTION:
          serviceImpl.streamingInvokeAction((com.giyeok.bibix.daemon.BibixDaemonApiProto.InvokeActionReq) request,
              (io.grpc.stub.StreamObserver<com.giyeok.bibix.daemon.BibixDaemonApiProto.StreamingActionEvent>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class BibixDaemonApiBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    BibixDaemonApiBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.giyeok.bibix.daemon.BibixDaemonApiProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("BibixDaemonApi");
    }
  }

  private static final class BibixDaemonApiFileDescriptorSupplier
      extends BibixDaemonApiBaseDescriptorSupplier {
    BibixDaemonApiFileDescriptorSupplier() {}
  }

  private static final class BibixDaemonApiMethodDescriptorSupplier
      extends BibixDaemonApiBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    BibixDaemonApiMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (BibixDaemonApiGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new BibixDaemonApiFileDescriptorSupplier())
              .addMethod(getGetRepoInfoMethod())
              .addMethod(getReloadScriptMethod())
              .addMethod(getBuildTargetMethod())
              .addMethod(getInvokeActionMethod())
              .addMethod(getStreamingInvokeActionMethod())
              .build();
        }
      }
    }
    return result;
  }
}
