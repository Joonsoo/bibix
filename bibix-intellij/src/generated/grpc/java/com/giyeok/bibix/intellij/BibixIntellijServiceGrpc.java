package com.giyeok.bibix.intellij;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.46.0)",
    comments = "Source: api.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class BibixIntellijServiceGrpc {

  private BibixIntellijServiceGrpc() {}

  public static final String SERVICE_NAME = "com.giyeok.bibix.intellij.BibixIntellijService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.giyeok.bibix.intellij.BibixIntellijProto.LoadProjectReq,
      com.giyeok.bibix.intellij.BibixIntellijProto.BibixProjectInfo> getLoadProjectMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "loadProject",
      requestType = com.giyeok.bibix.intellij.BibixIntellijProto.LoadProjectReq.class,
      responseType = com.giyeok.bibix.intellij.BibixIntellijProto.BibixProjectInfo.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.giyeok.bibix.intellij.BibixIntellijProto.LoadProjectReq,
      com.giyeok.bibix.intellij.BibixIntellijProto.BibixProjectInfo> getLoadProjectMethod() {
    io.grpc.MethodDescriptor<com.giyeok.bibix.intellij.BibixIntellijProto.LoadProjectReq, com.giyeok.bibix.intellij.BibixIntellijProto.BibixProjectInfo> getLoadProjectMethod;
    if ((getLoadProjectMethod = BibixIntellijServiceGrpc.getLoadProjectMethod) == null) {
      synchronized (BibixIntellijServiceGrpc.class) {
        if ((getLoadProjectMethod = BibixIntellijServiceGrpc.getLoadProjectMethod) == null) {
          BibixIntellijServiceGrpc.getLoadProjectMethod = getLoadProjectMethod =
              io.grpc.MethodDescriptor.<com.giyeok.bibix.intellij.BibixIntellijProto.LoadProjectReq, com.giyeok.bibix.intellij.BibixIntellijProto.BibixProjectInfo>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "loadProject"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.giyeok.bibix.intellij.BibixIntellijProto.LoadProjectReq.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.giyeok.bibix.intellij.BibixIntellijProto.BibixProjectInfo.getDefaultInstance()))
              .setSchemaDescriptor(new BibixIntellijServiceMethodDescriptorSupplier("loadProject"))
              .build();
        }
      }
    }
    return getLoadProjectMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static BibixIntellijServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<BibixIntellijServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<BibixIntellijServiceStub>() {
        @java.lang.Override
        public BibixIntellijServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new BibixIntellijServiceStub(channel, callOptions);
        }
      };
    return BibixIntellijServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static BibixIntellijServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<BibixIntellijServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<BibixIntellijServiceBlockingStub>() {
        @java.lang.Override
        public BibixIntellijServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new BibixIntellijServiceBlockingStub(channel, callOptions);
        }
      };
    return BibixIntellijServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static BibixIntellijServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<BibixIntellijServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<BibixIntellijServiceFutureStub>() {
        @java.lang.Override
        public BibixIntellijServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new BibixIntellijServiceFutureStub(channel, callOptions);
        }
      };
    return BibixIntellijServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class BibixIntellijServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void loadProject(com.giyeok.bibix.intellij.BibixIntellijProto.LoadProjectReq request,
        io.grpc.stub.StreamObserver<com.giyeok.bibix.intellij.BibixIntellijProto.BibixProjectInfo> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getLoadProjectMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getLoadProjectMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.giyeok.bibix.intellij.BibixIntellijProto.LoadProjectReq,
                com.giyeok.bibix.intellij.BibixIntellijProto.BibixProjectInfo>(
                  this, METHODID_LOAD_PROJECT)))
          .build();
    }
  }

  /**
   */
  public static final class BibixIntellijServiceStub extends io.grpc.stub.AbstractAsyncStub<BibixIntellijServiceStub> {
    private BibixIntellijServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected BibixIntellijServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new BibixIntellijServiceStub(channel, callOptions);
    }

    /**
     */
    public void loadProject(com.giyeok.bibix.intellij.BibixIntellijProto.LoadProjectReq request,
        io.grpc.stub.StreamObserver<com.giyeok.bibix.intellij.BibixIntellijProto.BibixProjectInfo> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getLoadProjectMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class BibixIntellijServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<BibixIntellijServiceBlockingStub> {
    private BibixIntellijServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected BibixIntellijServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new BibixIntellijServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.giyeok.bibix.intellij.BibixIntellijProto.BibixProjectInfo loadProject(com.giyeok.bibix.intellij.BibixIntellijProto.LoadProjectReq request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getLoadProjectMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class BibixIntellijServiceFutureStub extends io.grpc.stub.AbstractFutureStub<BibixIntellijServiceFutureStub> {
    private BibixIntellijServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected BibixIntellijServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new BibixIntellijServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.giyeok.bibix.intellij.BibixIntellijProto.BibixProjectInfo> loadProject(
        com.giyeok.bibix.intellij.BibixIntellijProto.LoadProjectReq request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getLoadProjectMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_LOAD_PROJECT = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final BibixIntellijServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(BibixIntellijServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_LOAD_PROJECT:
          serviceImpl.loadProject((com.giyeok.bibix.intellij.BibixIntellijProto.LoadProjectReq) request,
              (io.grpc.stub.StreamObserver<com.giyeok.bibix.intellij.BibixIntellijProto.BibixProjectInfo>) responseObserver);
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

  private static abstract class BibixIntellijServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    BibixIntellijServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.giyeok.bibix.intellij.BibixIntellijProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("BibixIntellijService");
    }
  }

  private static final class BibixIntellijServiceFileDescriptorSupplier
      extends BibixIntellijServiceBaseDescriptorSupplier {
    BibixIntellijServiceFileDescriptorSupplier() {}
  }

  private static final class BibixIntellijServiceMethodDescriptorSupplier
      extends BibixIntellijServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    BibixIntellijServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (BibixIntellijServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new BibixIntellijServiceFileDescriptorSupplier())
              .addMethod(getLoadProjectMethod())
              .build();
        }
      }
    }
    return result;
  }
}
