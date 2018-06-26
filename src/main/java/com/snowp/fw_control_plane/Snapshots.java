package source.com.snowp.fw_control_plane;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.protobuf.Message;
import envoy.api.v2.Cds;
import envoy.api.v2.Eds;
import envoy.api.v2.Lds;
import envoy.api.v2.Rds;
import io.envoyproxy.controlplane.cache.Resources;
import io.envoyproxy.controlplane.cache.Snapshot;
import java.util.stream.Collectors;

public class Snapshots {
  static Snapshot fromResourceMap(Multimap<String, Message> resources, String version) {
    return Snapshot.create(resources.get(Resources.CLUSTER_TYPE_URL)
            .stream()
            .map(x -> (Cds.Cluster) x)
            .collect(Collectors.toSet()),
        resources.get(Resources.ENDPOINT_TYPE_URL)
            .stream()
            .map(x -> (Eds.ClusterLoadAssignment) x)
            .collect(Collectors.toSet()), resources.get(Resources.LISTENER_TYPE_URL)
            .stream()
            .map(x -> (Lds.Listener) x)
            .collect(Collectors.toSet()),
        resources.get(Resources.ROUTE_TYPE_URL)
            .stream()
            .map(x -> (Rds.RouteConfiguration) x)
            .collect(Collectors.toSet()), version);
  }

  static Multimap<String, Message> toMultiMap(Snapshot snapshot) {
    HashMultimap<String, Message> map = HashMultimap.create();

    for (String typeUrl : Resources.TYPE_URLS) {
      map.putAll(typeUrl, snapshot.resources(typeUrl).values());
    }

    return map;
  }

}
