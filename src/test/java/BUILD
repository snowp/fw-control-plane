java_library(
    name = "file_configuration",
    srcs = [
        "com/snowp/fw_control_plane/FileConfigurationManagerTest.java"
    ],
    deps = [
        '//3rdparty/jvm/junit:junit',
        '//3rdparty/jvm/com/google/guava:guava',
        '//3rdparty/jvm/com/google/protobuf:protobuf_java',
        '//3rdparty/jvm/com/google/protobuf:protobuf_java_util',
        '//3rdparty/jvm/io/envoyproxy/controlplane:api',
        '//3rdparty/jvm/io/envoyproxy/controlplane:cache',
        '//src/main/java:main'
    ]
)

java_test(
    name = "file_configuration_test",
    test_class = 'com.snowp.fw_control_plane.FileConfigurationManagerTest',
    runtime_deps = [
        ':file_configuration'
        ],
)