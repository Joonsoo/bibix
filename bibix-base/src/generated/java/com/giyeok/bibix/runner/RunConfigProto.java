// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: run_config.proto

package com.giyeok.bibix.runner;

public final class RunConfigProto {
  private RunConfigProto() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  public interface RunConfigOrBuilder extends
      // @@protoc_insertion_point(interface_extends:com.giyeok.bibix.runner.RunConfig)
      com.google.protobuf.MessageOrBuilder {

    /**
     * <code>int32 max_threads = 1;</code>
     * @return The maxThreads.
     */
    int getMaxThreads();
  }
  /**
   * Protobuf type {@code com.giyeok.bibix.runner.RunConfig}
   */
  public static final class RunConfig extends
      com.google.protobuf.GeneratedMessageV3 implements
      // @@protoc_insertion_point(message_implements:com.giyeok.bibix.runner.RunConfig)
      RunConfigOrBuilder {
  private static final long serialVersionUID = 0L;
    // Use RunConfig.newBuilder() to construct.
    private RunConfig(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }
    private RunConfig() {
    }

    @java.lang.Override
    @SuppressWarnings({"unused"})
    protected java.lang.Object newInstance(
        UnusedPrivateParameter unused) {
      return new RunConfig();
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet
    getUnknownFields() {
      return this.unknownFields;
    }
    private RunConfig(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      this();
      if (extensionRegistry == null) {
        throw new java.lang.NullPointerException();
      }
      com.google.protobuf.UnknownFieldSet.Builder unknownFields =
          com.google.protobuf.UnknownFieldSet.newBuilder();
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            case 8: {

              maxThreads_ = input.readInt32();
              break;
            }
            default: {
              if (!parseUnknownField(
                  input, unknownFields, extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(
            e).setUnfinishedMessage(this);
      } finally {
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return com.giyeok.bibix.runner.RunConfigProto.internal_static_com_giyeok_bibix_runner_RunConfig_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.giyeok.bibix.runner.RunConfigProto.internal_static_com_giyeok_bibix_runner_RunConfig_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.giyeok.bibix.runner.RunConfigProto.RunConfig.class, com.giyeok.bibix.runner.RunConfigProto.RunConfig.Builder.class);
    }

    public static final int MAX_THREADS_FIELD_NUMBER = 1;
    private int maxThreads_;
    /**
     * <code>int32 max_threads = 1;</code>
     * @return The maxThreads.
     */
    @java.lang.Override
    public int getMaxThreads() {
      return maxThreads_;
    }

    private byte memoizedIsInitialized = -1;
    @java.lang.Override
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1) return true;
      if (isInitialized == 0) return false;

      memoizedIsInitialized = 1;
      return true;
    }

    @java.lang.Override
    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      if (maxThreads_ != 0) {
        output.writeInt32(1, maxThreads_);
      }
      unknownFields.writeTo(output);
    }

    @java.lang.Override
    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1) return size;

      size = 0;
      if (maxThreads_ != 0) {
        size += com.google.protobuf.CodedOutputStream
          .computeInt32Size(1, maxThreads_);
      }
      size += unknownFields.getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object obj) {
      if (obj == this) {
       return true;
      }
      if (!(obj instanceof com.giyeok.bibix.runner.RunConfigProto.RunConfig)) {
        return super.equals(obj);
      }
      com.giyeok.bibix.runner.RunConfigProto.RunConfig other = (com.giyeok.bibix.runner.RunConfigProto.RunConfig) obj;

      if (getMaxThreads()
          != other.getMaxThreads()) return false;
      if (!unknownFields.equals(other.unknownFields)) return false;
      return true;
    }

    @java.lang.Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      hash = (37 * hash) + MAX_THREADS_FIELD_NUMBER;
      hash = (53 * hash) + getMaxThreads();
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static com.giyeok.bibix.runner.RunConfigProto.RunConfig parseFrom(
        java.nio.ByteBuffer data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static com.giyeok.bibix.runner.RunConfigProto.RunConfig parseFrom(
        java.nio.ByteBuffer data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static com.giyeok.bibix.runner.RunConfigProto.RunConfig parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static com.giyeok.bibix.runner.RunConfigProto.RunConfig parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static com.giyeok.bibix.runner.RunConfigProto.RunConfig parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static com.giyeok.bibix.runner.RunConfigProto.RunConfig parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static com.giyeok.bibix.runner.RunConfigProto.RunConfig parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input);
    }
    public static com.giyeok.bibix.runner.RunConfigProto.RunConfig parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input, extensionRegistry);
    }
    public static com.giyeok.bibix.runner.RunConfigProto.RunConfig parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseDelimitedWithIOException(PARSER, input);
    }
    public static com.giyeok.bibix.runner.RunConfigProto.RunConfig parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }
    public static com.giyeok.bibix.runner.RunConfigProto.RunConfig parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input);
    }
    public static com.giyeok.bibix.runner.RunConfigProto.RunConfig parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input, extensionRegistry);
    }

    @java.lang.Override
    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }
    public static Builder newBuilder(com.giyeok.bibix.runner.RunConfigProto.RunConfig prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }
    @java.lang.Override
    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE
          ? new Builder() : new Builder().mergeFrom(this);
    }

    @java.lang.Override
    protected Builder newBuilderForType(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }
    /**
     * Protobuf type {@code com.giyeok.bibix.runner.RunConfig}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
        // @@protoc_insertion_point(builder_implements:com.giyeok.bibix.runner.RunConfig)
        com.giyeok.bibix.runner.RunConfigProto.RunConfigOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
          getDescriptor() {
        return com.giyeok.bibix.runner.RunConfigProto.internal_static_com_giyeok_bibix_runner_RunConfig_descriptor;
      }

      @java.lang.Override
      protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
          internalGetFieldAccessorTable() {
        return com.giyeok.bibix.runner.RunConfigProto.internal_static_com_giyeok_bibix_runner_RunConfig_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                com.giyeok.bibix.runner.RunConfigProto.RunConfig.class, com.giyeok.bibix.runner.RunConfigProto.RunConfig.Builder.class);
      }

      // Construct using com.giyeok.bibix.runner.RunConfigProto.RunConfig.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(
          com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }
      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessageV3
                .alwaysUseFieldBuilders) {
        }
      }
      @java.lang.Override
      public Builder clear() {
        super.clear();
        maxThreads_ = 0;

        return this;
      }

      @java.lang.Override
      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return com.giyeok.bibix.runner.RunConfigProto.internal_static_com_giyeok_bibix_runner_RunConfig_descriptor;
      }

      @java.lang.Override
      public com.giyeok.bibix.runner.RunConfigProto.RunConfig getDefaultInstanceForType() {
        return com.giyeok.bibix.runner.RunConfigProto.RunConfig.getDefaultInstance();
      }

      @java.lang.Override
      public com.giyeok.bibix.runner.RunConfigProto.RunConfig build() {
        com.giyeok.bibix.runner.RunConfigProto.RunConfig result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      @java.lang.Override
      public com.giyeok.bibix.runner.RunConfigProto.RunConfig buildPartial() {
        com.giyeok.bibix.runner.RunConfigProto.RunConfig result = new com.giyeok.bibix.runner.RunConfigProto.RunConfig(this);
        result.maxThreads_ = maxThreads_;
        onBuilt();
        return result;
      }

      @java.lang.Override
      public Builder clone() {
        return super.clone();
      }
      @java.lang.Override
      public Builder setField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          java.lang.Object value) {
        return super.setField(field, value);
      }
      @java.lang.Override
      public Builder clearField(
          com.google.protobuf.Descriptors.FieldDescriptor field) {
        return super.clearField(field);
      }
      @java.lang.Override
      public Builder clearOneof(
          com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return super.clearOneof(oneof);
      }
      @java.lang.Override
      public Builder setRepeatedField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          int index, java.lang.Object value) {
        return super.setRepeatedField(field, index, value);
      }
      @java.lang.Override
      public Builder addRepeatedField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          java.lang.Object value) {
        return super.addRepeatedField(field, value);
      }
      @java.lang.Override
      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof com.giyeok.bibix.runner.RunConfigProto.RunConfig) {
          return mergeFrom((com.giyeok.bibix.runner.RunConfigProto.RunConfig)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(com.giyeok.bibix.runner.RunConfigProto.RunConfig other) {
        if (other == com.giyeok.bibix.runner.RunConfigProto.RunConfig.getDefaultInstance()) return this;
        if (other.getMaxThreads() != 0) {
          setMaxThreads(other.getMaxThreads());
        }
        this.mergeUnknownFields(other.unknownFields);
        onChanged();
        return this;
      }

      @java.lang.Override
      public final boolean isInitialized() {
        return true;
      }

      @java.lang.Override
      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        com.giyeok.bibix.runner.RunConfigProto.RunConfig parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (com.giyeok.bibix.runner.RunConfigProto.RunConfig) e.getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }

      private int maxThreads_ ;
      /**
       * <code>int32 max_threads = 1;</code>
       * @return The maxThreads.
       */
      @java.lang.Override
      public int getMaxThreads() {
        return maxThreads_;
      }
      /**
       * <code>int32 max_threads = 1;</code>
       * @param value The maxThreads to set.
       * @return This builder for chaining.
       */
      public Builder setMaxThreads(int value) {
        
        maxThreads_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>int32 max_threads = 1;</code>
       * @return This builder for chaining.
       */
      public Builder clearMaxThreads() {
        
        maxThreads_ = 0;
        onChanged();
        return this;
      }
      @java.lang.Override
      public final Builder setUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.setUnknownFields(unknownFields);
      }

      @java.lang.Override
      public final Builder mergeUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.mergeUnknownFields(unknownFields);
      }


      // @@protoc_insertion_point(builder_scope:com.giyeok.bibix.runner.RunConfig)
    }

    // @@protoc_insertion_point(class_scope:com.giyeok.bibix.runner.RunConfig)
    private static final com.giyeok.bibix.runner.RunConfigProto.RunConfig DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new com.giyeok.bibix.runner.RunConfigProto.RunConfig();
    }

    public static com.giyeok.bibix.runner.RunConfigProto.RunConfig getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<RunConfig>
        PARSER = new com.google.protobuf.AbstractParser<RunConfig>() {
      @java.lang.Override
      public RunConfig parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
        return new RunConfig(input, extensionRegistry);
      }
    };

    public static com.google.protobuf.Parser<RunConfig> parser() {
      return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<RunConfig> getParserForType() {
      return PARSER;
    }

    @java.lang.Override
    public com.giyeok.bibix.runner.RunConfigProto.RunConfig getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  private static final com.google.protobuf.Descriptors.Descriptor
    internal_static_com_giyeok_bibix_runner_RunConfig_descriptor;
  private static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_com_giyeok_bibix_runner_RunConfig_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\020run_config.proto\022\027com.giyeok.bibix.run" +
      "ner\" \n\tRunConfig\022\023\n\013max_threads\030\001 \001(\005B\020B" +
      "\016RunConfigProtob\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        });
    internal_static_com_giyeok_bibix_runner_RunConfig_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_com_giyeok_bibix_runner_RunConfig_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_com_giyeok_bibix_runner_RunConfig_descriptor,
        new java.lang.String[] { "MaxThreads", });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
