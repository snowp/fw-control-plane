java_library(
    name = "main",
    srcs = [
        "com/snowp/fw_control_plane/Main.java",
        "com/snowp/fw_control_plane/FileConfigurationManager.java",
        "com/snowp/fw_control_plane/ResourceFileReader.java"
    ],
    deps = [
        "@io_envoyproxy_controlplane_cache//jar",
        "@io_envoyproxy_controlplane_api//jar",
        "@com_google_protobuf_protobuf_java_util//jar",
        "@com_google_protobuf//:protobuf_java",
        "@com_google_guava_guava//jar",
        "@org_slf4j_slf4j_api//jar",
    ],
    visibility = ['//visibility:public']
)