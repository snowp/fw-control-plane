java_library(
    name = "api",
    exports = [
        "//external:jar/io/envoyproxy/controlplane/api"
    ],
    runtime_deps = [
        "//3rdparty/jvm/io/grpc:grpc_protobuf",
        "//3rdparty/jvm/io/grpc:grpc_stub"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "cache",
    exports = [
        "//external:jar/io/envoyproxy/controlplane/cache"
    ],
    runtime_deps = [
        "//3rdparty/jvm/io/projectreactor:reactor_core",
        "//3rdparty/jvm/org/slf4j:slf4j_api",
        ":api"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "server",
    exports = [
        "//external:jar/io/envoyproxy/controlplane/server"
    ],
    runtime_deps = [
        ":cache"
    ],
    visibility = [
        "//visibility:public"
    ]
)


