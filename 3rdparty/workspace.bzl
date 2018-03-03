# Do not edit. bazel-deps autogenerates this file from ../fw-control-plane/dependencies.yaml.

def declare_maven(hash):
    native.maven_jar(
        name = hash["name"],
        artifact = hash["artifact"],
        sha1 = hash["sha1"],
        repository = hash["repository"]
    )
    native.bind(
        name = hash["bind"],
        actual = hash["actual"]
    )

def maven_dependencies(callback = declare_maven):
    callback({"artifact": "com.google.api.grpc:proto-google-common-protos:1.0.0", "lang": "java", "sha1": "86f070507e28b930e50d218ee5b6788ef0dd05e6", "repository": "https://repo.maven.apache.org/maven2/", "name": "com_google_api_grpc_proto_google_common_protos", "actual": "@com_google_api_grpc_proto_google_common_protos//jar", "bind": "jar/com/google/api/grpc/proto_google_common_protos"})
# duplicates in com.google.code.findbugs:jsr305 promoted to 3.0.0
# - com.google.guava:guava:24.0-jre wanted version 1.3.9
# - io.grpc:grpc-core:1.10.0 wanted version 3.0.0
    callback({"artifact": "com.google.code.findbugs:jsr305:3.0.0", "lang": "java", "sha1": "5871fb60dc68d67da54a663c3fd636a10a532948", "repository": "https://repo.maven.apache.org/maven2/", "name": "com_google_code_findbugs_jsr305", "actual": "@com_google_code_findbugs_jsr305//jar", "bind": "jar/com/google/code/findbugs/jsr305"})
    callback({"artifact": "com.google.code.gson:gson:2.7", "lang": "java", "sha1": "751f548c85fa49f330cecbb1875893f971b33c4e", "repository": "https://repo.maven.apache.org/maven2/", "name": "com_google_code_gson_gson", "actual": "@com_google_code_gson_gson//jar", "bind": "jar/com/google/code/gson/gson"})
# duplicates in com.google.errorprone:error_prone_annotations promoted to 2.1.3
# - io.grpc:grpc-core:1.10.0 wanted version 2.1.2
# - com.google.guava:guava:24.0-jre wanted version 2.1.3
    callback({"artifact": "com.google.errorprone:error_prone_annotations:2.1.3", "lang": "java", "sha1": "39b109f2cd352b2d71b52a3b5a1a9850e1dc304b", "repository": "https://repo.maven.apache.org/maven2/", "name": "com_google_errorprone_error_prone_annotations", "actual": "@com_google_errorprone_error_prone_annotations//jar", "bind": "jar/com/google/errorprone/error_prone_annotations"})
    callback({"artifact": "com.google.guava:guava:24.0-jre", "lang": "java", "sha1": "041ac1e74d6b4e1ea1f027139cffeb536c732a81", "repository": "https://repo.maven.apache.org/maven2/", "name": "com_google_guava_guava", "actual": "@com_google_guava_guava//jar", "bind": "jar/com/google/guava/guava"})
    callback({"artifact": "com.google.j2objc:j2objc-annotations:1.1", "lang": "java", "sha1": "ed28ded51a8b1c6b112568def5f4b455e6809019", "repository": "https://repo.maven.apache.org/maven2/", "name": "com_google_j2objc_j2objc_annotations", "actual": "@com_google_j2objc_j2objc_annotations//jar", "bind": "jar/com/google/j2objc/j2objc_annotations"})
    callback({"artifact": "com.google.protobuf:protobuf-java-util:3.5.1", "lang": "java", "sha1": "6e40a6a3f52455bd633aa2a0dba1a416e62b4575", "repository": "https://repo.maven.apache.org/maven2/", "name": "com_google_protobuf_protobuf_java_util", "actual": "@com_google_protobuf_protobuf_java_util//jar", "bind": "jar/com/google/protobuf/protobuf_java_util"})
    callback({"artifact": "com.google.protobuf:protobuf-java:3.5.1", "lang": "java", "sha1": "8c3492f7662fa1cbf8ca76a0f5eb1146f7725acd", "repository": "https://repo.maven.apache.org/maven2/", "name": "com_google_protobuf_protobuf_java", "actual": "@com_google_protobuf_protobuf_java//jar", "bind": "jar/com/google/protobuf/protobuf_java"})
    callback({"artifact": "io.envoyproxy.controlplane:api:0.0.7", "lang": "java", "sha1": "7ec0d8c6c356d267117cc072a1db61d2ae946a18", "repository": "https://repo.maven.apache.org/maven2/", "name": "io_envoyproxy_controlplane_api", "actual": "@io_envoyproxy_controlplane_api//jar", "bind": "jar/io/envoyproxy/controlplane/api"})
    callback({"artifact": "io.envoyproxy.controlplane:cache:0.0.7", "lang": "java", "sha1": "e4ae670526e643a872dfc139298e295527b2610d", "repository": "https://repo.maven.apache.org/maven2/", "name": "io_envoyproxy_controlplane_cache", "actual": "@io_envoyproxy_controlplane_cache//jar", "bind": "jar/io/envoyproxy/controlplane/cache"})
    callback({"artifact": "io.envoyproxy.controlplane:server:0.0.7", "lang": "java", "sha1": "54acb1420527ecbed4d2faa8e28d6a19bca3e9f3", "repository": "https://repo.maven.apache.org/maven2/", "name": "io_envoyproxy_controlplane_server", "actual": "@io_envoyproxy_controlplane_server//jar", "bind": "jar/io/envoyproxy/controlplane/server"})
    callback({"artifact": "io.grpc:grpc-context:1.10.0", "lang": "java", "sha1": "da0a701be6ba04aff0bd54ca3db8248d8f2eaafc", "repository": "https://repo.maven.apache.org/maven2/", "name": "io_grpc_grpc_context", "actual": "@io_grpc_grpc_context//jar", "bind": "jar/io/grpc/grpc_context"})
    callback({"artifact": "io.grpc:grpc-core:1.10.0", "lang": "java", "sha1": "8976afebf2a6530574a71bc1260920ce910c2292", "repository": "https://repo.maven.apache.org/maven2/", "name": "io_grpc_grpc_core", "actual": "@io_grpc_grpc_core//jar", "bind": "jar/io/grpc/grpc_core"})
    callback({"artifact": "io.grpc:grpc-protobuf-lite:1.10.0", "lang": "java", "sha1": "b8e40dd308dc370e64bd2c337bb2761a03299a7f", "repository": "https://repo.maven.apache.org/maven2/", "name": "io_grpc_grpc_protobuf_lite", "actual": "@io_grpc_grpc_protobuf_lite//jar", "bind": "jar/io/grpc/grpc_protobuf_lite"})
    callback({"artifact": "io.grpc:grpc-protobuf:1.10.0", "lang": "java", "sha1": "64098f046f227b47238bc747e3cee6c7fc087bb8", "repository": "https://repo.maven.apache.org/maven2/", "name": "io_grpc_grpc_protobuf", "actual": "@io_grpc_grpc_protobuf//jar", "bind": "jar/io/grpc/grpc_protobuf"})
    callback({"artifact": "io.grpc:grpc-stub:1.10.0", "lang": "java", "sha1": "d022706796b0820d388f83571da160fb8d280ded", "repository": "https://repo.maven.apache.org/maven2/", "name": "io_grpc_grpc_stub", "actual": "@io_grpc_grpc_stub//jar", "bind": "jar/io/grpc/grpc_stub"})
    callback({"artifact": "io.opencensus:opencensus-api:0.11.0", "lang": "java", "sha1": "c1ff1f0d737a689d900a3e2113ddc29847188c64", "repository": "https://repo.maven.apache.org/maven2/", "name": "io_opencensus_opencensus_api", "actual": "@io_opencensus_opencensus_api//jar", "bind": "jar/io/opencensus/opencensus_api"})
    callback({"artifact": "io.opencensus:opencensus-contrib-grpc-metrics:0.11.0", "lang": "java", "sha1": "d57b877f1a28a613452d45e35c7faae5af585258", "repository": "https://repo.maven.apache.org/maven2/", "name": "io_opencensus_opencensus_contrib_grpc_metrics", "actual": "@io_opencensus_opencensus_contrib_grpc_metrics//jar", "bind": "jar/io/opencensus/opencensus_contrib_grpc_metrics"})
    callback({"artifact": "io.projectreactor:reactor-core:3.1.3.RELEASE", "lang": "java", "sha1": "ca04584098c515657db3b58a09a0532b2341e13b", "repository": "https://repo.maven.apache.org/maven2/", "name": "io_projectreactor_reactor_core", "actual": "@io_projectreactor_reactor_core//jar", "bind": "jar/io/projectreactor/reactor_core"})
    callback({"artifact": "junit:junit:4.12", "lang": "java", "sha1": "2973d150c0dc1fefe998f834810d68f278ea58ec", "repository": "https://repo.maven.apache.org/maven2/", "name": "junit_junit", "actual": "@junit_junit//jar", "bind": "jar/junit/junit"})
    callback({"artifact": "org.checkerframework:checker-compat-qual:2.0.0", "lang": "java", "sha1": "fc89b03860d11d6213d0154a62bcd1c2f69b9efa", "repository": "https://repo.maven.apache.org/maven2/", "name": "org_checkerframework_checker_compat_qual", "actual": "@org_checkerframework_checker_compat_qual//jar", "bind": "jar/org/checkerframework/checker_compat_qual"})
    callback({"artifact": "org.codehaus.mojo:animal-sniffer-annotations:1.14", "lang": "java", "sha1": "775b7e22fb10026eed3f86e8dc556dfafe35f2d5", "repository": "https://repo.maven.apache.org/maven2/", "name": "org_codehaus_mojo_animal_sniffer_annotations", "actual": "@org_codehaus_mojo_animal_sniffer_annotations//jar", "bind": "jar/org/codehaus/mojo/animal_sniffer_annotations"})
    callback({"artifact": "org.hamcrest:hamcrest-core:1.3", "lang": "java", "sha1": "42a25dc3219429f0e5d060061f71acb49bf010a0", "repository": "https://repo.maven.apache.org/maven2/", "name": "org_hamcrest_hamcrest_core", "actual": "@org_hamcrest_hamcrest_core//jar", "bind": "jar/org/hamcrest/hamcrest_core"})
    callback({"artifact": "org.reactivestreams:reactive-streams:1.0.2", "lang": "java", "sha1": "323964c36556eb0e6209f65c1cef72b53b461ab8", "repository": "https://repo.maven.apache.org/maven2/", "name": "org_reactivestreams_reactive_streams", "actual": "@org_reactivestreams_reactive_streams//jar", "bind": "jar/org/reactivestreams/reactive_streams"})
    callback({"artifact": "org.slf4j:slf4j-api:1.7.25", "lang": "java", "sha1": "da76ca59f6a57ee3102f8f9bd9cee742973efa8a", "repository": "https://repo.maven.apache.org/maven2/", "name": "org_slf4j_slf4j_api", "actual": "@org_slf4j_slf4j_api//jar", "bind": "jar/org/slf4j/slf4j_api"})
