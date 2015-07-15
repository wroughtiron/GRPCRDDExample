package anything.protocode;

import static io.grpc.stub.Calls.createMethodDescriptor;
import static io.grpc.stub.Calls.asyncServerStreamingCall;
import static io.grpc.stub.Calls.blockingServerStreamingCall;
import static io.grpc.stub.ServerCalls.createMethodDefinition;
import static io.grpc.stub.ServerCalls.asyncUnaryRequestCall;

@javax.annotation.Generated("by gRPC proto compiler")
public class MetadataStoreGrpc {

  private static final io.grpc.stub.Method<anything.protocode.Interface.Criteria,
      anything.protocode.Interface.Result> METHOD_QUERY =
      io.grpc.stub.Method.create(
          io.grpc.MethodType.SERVER_STREAMING, "Query",
          io.grpc.protobuf.ProtoUtils.marshaller(anything.protocode.Interface.Criteria.PARSER),
          io.grpc.protobuf.ProtoUtils.marshaller(anything.protocode.Interface.Result.PARSER));

  public static MetadataStoreStub newStub(io.grpc.Channel channel) {
    return new MetadataStoreStub(channel, CONFIG);
  }

  public static MetadataStoreBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new MetadataStoreBlockingStub(channel, CONFIG);
  }

  public static MetadataStoreFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new MetadataStoreFutureStub(channel, CONFIG);
  }

  public static final MetadataStoreServiceDescriptor CONFIG =
      new MetadataStoreServiceDescriptor();

  @javax.annotation.concurrent.Immutable
  public static class MetadataStoreServiceDescriptor extends
      io.grpc.stub.AbstractServiceDescriptor<MetadataStoreServiceDescriptor> {
    public final io.grpc.MethodDescriptor<anything.protocode.Interface.Criteria,
        anything.protocode.Interface.Result> query;

    private MetadataStoreServiceDescriptor() {
      query = createMethodDescriptor(
          "MetadataStoreInterface.MetadataStore", METHOD_QUERY);
    }

    @SuppressWarnings("unchecked")
    private MetadataStoreServiceDescriptor(
        java.util.Map<java.lang.String, io.grpc.MethodDescriptor<?, ?>> methodMap) {
      query = (io.grpc.MethodDescriptor<anything.protocode.Interface.Criteria,
          anything.protocode.Interface.Result>) methodMap.get(
          CONFIG.query.getName());
    }

    @java.lang.Override
    protected MetadataStoreServiceDescriptor build(
        java.util.Map<java.lang.String, io.grpc.MethodDescriptor<?, ?>> methodMap) {
      return new MetadataStoreServiceDescriptor(methodMap);
    }

    @java.lang.Override
    public com.google.common.collect.ImmutableList<io.grpc.MethodDescriptor<?, ?>> methods() {
      return com.google.common.collect.ImmutableList.<io.grpc.MethodDescriptor<?, ?>>of(
          query);
    }
  }

  public static interface MetadataStore {

    public void query(anything.protocode.Interface.Criteria request,
        io.grpc.stub.StreamObserver<anything.protocode.Interface.Result> responseObserver);
  }

  public static interface MetadataStoreBlockingClient {

    public java.util.Iterator<anything.protocode.Interface.Result> query(
        anything.protocode.Interface.Criteria request);
  }

  public static interface MetadataStoreFutureClient {
  }

  public static class MetadataStoreStub extends
      io.grpc.stub.AbstractStub<MetadataStoreStub, MetadataStoreServiceDescriptor>
      implements MetadataStore {
    private MetadataStoreStub(io.grpc.Channel channel,
        MetadataStoreServiceDescriptor config) {
      super(channel, config);
    }

    @java.lang.Override
    protected MetadataStoreStub build(io.grpc.Channel channel,
        MetadataStoreServiceDescriptor config) {
      return new MetadataStoreStub(channel, config);
    }

    @java.lang.Override
    public void query(anything.protocode.Interface.Criteria request,
        io.grpc.stub.StreamObserver<anything.protocode.Interface.Result> responseObserver) {
      asyncServerStreamingCall(
          channel.newCall(config.query), request, responseObserver);
    }
  }

  public static class MetadataStoreBlockingStub extends
      io.grpc.stub.AbstractStub<MetadataStoreBlockingStub, MetadataStoreServiceDescriptor>
      implements MetadataStoreBlockingClient {
    private MetadataStoreBlockingStub(io.grpc.Channel channel,
        MetadataStoreServiceDescriptor config) {
      super(channel, config);
    }

    @java.lang.Override
    protected MetadataStoreBlockingStub build(io.grpc.Channel channel,
        MetadataStoreServiceDescriptor config) {
      return new MetadataStoreBlockingStub(channel, config);
    }

    @java.lang.Override
    public java.util.Iterator<anything.protocode.Interface.Result> query(
        anything.protocode.Interface.Criteria request) {
      return blockingServerStreamingCall(
          channel.newCall(config.query), request);
    }
  }

  public static class MetadataStoreFutureStub extends
      io.grpc.stub.AbstractStub<MetadataStoreFutureStub, MetadataStoreServiceDescriptor>
      implements MetadataStoreFutureClient {
    private MetadataStoreFutureStub(io.grpc.Channel channel,
        MetadataStoreServiceDescriptor config) {
      super(channel, config);
    }

    @java.lang.Override
    protected MetadataStoreFutureStub build(io.grpc.Channel channel,
        MetadataStoreServiceDescriptor config) {
      return new MetadataStoreFutureStub(channel, config);
    }
  }

  public static io.grpc.ServerServiceDefinition bindService(
      final MetadataStore serviceImpl) {
    return io.grpc.ServerServiceDefinition.builder("MetadataStoreInterface.MetadataStore")
      .addMethod(createMethodDefinition(
          METHOD_QUERY,
          asyncUnaryRequestCall(
            new io.grpc.stub.ServerCalls.UnaryRequestMethod<
                anything.protocode.Interface.Criteria,
                anything.protocode.Interface.Result>() {
              @java.lang.Override
              public void invoke(
                  anything.protocode.Interface.Criteria request,
                  io.grpc.stub.StreamObserver<anything.protocode.Interface.Result> responseObserver) {
                serviceImpl.query(request, responseObserver);
              }
            }))).build();
  }
}
