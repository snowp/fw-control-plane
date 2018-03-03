package source.com.snowp.fw_control_plane;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import envoy.api.v2.Cds;
import envoy.api.v2.Eds;
import envoy.api.v2.Lds;
import envoy.api.v2.Rds;
import io.envoyproxy.controlplane.cache.ResourceType;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Supplier;

public class ResourceFileReader {

  private static final Map<ResourceType, Supplier<Message.Builder>> resourceConstructors =
      ImmutableMap.of(
          ResourceType.CLUSTER, Cds.Cluster::newBuilder,
          ResourceType.ENDPOINT, Eds.ClusterLoadAssignment::newBuilder,
          ResourceType.LISTENER, Lds.Listener::newBuilder,
          ResourceType.ROUTE, Rds.RouteConfiguration::newBuilder
      );

  Message readResource(Path path, ResourceType resourceType) {
    FileInputStream fileInputStream;
    Message.Builder resourceBuilder = resourceConstructors.get(resourceType).get();

    try {
      fileInputStream = new FileInputStream(path.toFile());
      JsonFormat.parser().merge(new InputStreamReader(fileInputStream),
          resourceBuilder);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return resourceBuilder.build();
  }
}
