workspace(name = "fw_control_plane")

# todo(snowp): use bazel-deps

JAVA_CONTROL_PLANE_REV='0.0.7'

http_archive(
    name = "com_google_protobuf",
    strip_prefix = "protobuf-3.5.1",
    urls = ["https://github.com/google/protobuf/archive/v3.5.1.zip"],
)

maven_jar(
    name = 'io_envoyproxy_controlplane_cache',
    artifact = 'io.envoyproxy.controlplane:cache:' + JAVA_CONTROL_PLANE_REV,
    )

maven_jar(
    name = 'io_envoyproxy_controlplane_api',
    artifact = 'io.envoyproxy.controlplane:api:' + JAVA_CONTROL_PLANE_REV,
    )

maven_jar(
    name = 'io_envoyproxy_controlplane_server',
    artifact = 'io.envoyproxy.controlplane:api:' + JAVA_CONTROL_PLANE_REV,
    )

maven_jar(
    name = 'com_google_protobuf_protobuf_java_util',
    artifact = 'com.google.protobuf:protobuf-java-util:3.5.1',
)

maven_jar(
    name = 'com_google_guava_guava',
    artifact = 'com.google.guava:guava:24.0-jre'
)

maven_jar(
    name = 'org_slf4j_slf4j_api',
    artifact = 'org.slf4j:slf4j-api:1.7.25'
)