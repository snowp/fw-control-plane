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
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ResourceFileReader {

  private static final Logger logger = Logger.getLogger(ResourceFileReader.class.getName());

  private static final Map<ResourceType, Supplier<Message.Builder>> resourceConstructors =
      ImmutableMap.of(
          ResourceType.CLUSTER, Cds.Cluster::newBuilder,
          ResourceType.ENDPOINT, Eds.ClusterLoadAssignment::newBuilder,
          ResourceType.LISTENER, Lds.Listener::newBuilder,
          ResourceType.ROUTE, Rds.RouteConfiguration::newBuilder
      );

  Optional<Message> readResource(Path path, ResourceType resourceType) {
    FileInputStream fileInputStream;
    Message.Builder resourceBuilder = resourceConstructors.get(resourceType).get();

    try {
      fileInputStream = new FileInputStream(path.toFile());
      JsonFormat.parser()
          .ignoringUnknownFields()
          .merge(new InputStreamReader(fileInputStream), resourceBuilder);
    } catch (IOException e) {
      logger.log(Level.WARNING, "failed to parse json at path=" + path.toString(), e);

      return Optional.empty();
    }

    return Optional.of(resourceBuilder.build());
  }
}
