java_library(
    name = "main",
    srcs = [
        "com/snowp/fw_control_plane/Main.java",
        "com/snowp/fw_control_plane/FileConfigurationManager.java",
        "com/snowp/fw_control_plane/ResourceFileLoader.java",
        "com/snowp/fw_control_plane/Snapshots.java",
        "com/snowp/fw_control_plane/cli/Args.java"
    ],
    deps = [
        "//3rdparty/jvm/io/envoyproxy/controlplane:api",
        "//3rdparty/jvm/io/envoyproxy/controlplane:cache",
        "//3rdparty/jvm/io/envoyproxy/controlplane:server",
        "//3rdparty/jvm/com/beust:jcommander",
        "//3rdparty/jvm/io/grpc:grpc_core",
        "//3rdparty/jvm/io/grpc:grpc_netty",
        "//3rdparty/jvm/com/google/protobuf:protobuf_java",
        "//3rdparty/jvm/com/google/protobuf:protobuf_java_util",
        "//3rdparty/jvm/com/google/guava:guava",
    ],
    visibility = ['//visibility:public']
)