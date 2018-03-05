package source.com.snowp.fw_control_plane;

import io.envoyproxy.controlplane.cache.SimpleCache;
import io.envoyproxy.controlplane.cache.Snapshot;
import io.envoyproxy.controlplane.server.DiscoveryServer;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class Main {
  public static void main(String[] args) throws Exception {
    SimpleCache cache = new SimpleCache(null, g -> "foo");

    Path configDirectory = Paths.get(System.getProperty("user.dir"), args[0].split("/"));

    // cheap and dirty way of ensuring we bump the version
    AtomicLong counter = new AtomicLong();

    FileConfigurationManager fileConfigurationManager =
        new FileConfigurationManager(configDirectory,
            ((group, resources) -> cache.setSnapshot(group,
                Snapshot.create(resources, ((Long) counter.incrementAndGet()).toString()))),
            new ResourceFileReader(),
            Executors.newSingleThreadExecutor());

    Thread fwThread = new Thread(() -> {
      while (true) {
        try {
          fileConfigurationManager.processFileEvents();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          // if the main thread interrupts us it's time to shut down
          return;
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    });

    DiscoveryServer discoveryServer = new DiscoveryServer(cache);

    Server grpcServer = NettyServerBuilder.forPort(5555)
        .addService(discoveryServer.getEndpointDiscoveryServiceImpl())
        .addService(discoveryServer.getRouteDiscoveryServiceImpl())
        .addService(discoveryServer.getListenerDiscoveryServiceImpl())
        .addService(discoveryServer.getClusterDiscoveryServiceImpl())
        .addService(discoveryServer.getEndpointDiscoveryServiceImpl())
        .build();

    fwThread.start();
    grpcServer.start();

    fwThread.join();
  }
}
