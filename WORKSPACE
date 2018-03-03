workspace(name = "fw_control_plane")

# todo(snowp): use bazel-deps

JAVA_CONTROL_PLANE_REV='0.0.7'

http_archive(
    name = "com_google_protobuf",
    strip_prefix = "protobuf-3.5.1",
    urls = ["https://github.com/google/protobuf/archive/v3.5.1.zip"],
)

load("//3rdparty:workspace.bzl", "maven_dependencies")

maven_dependencies()
