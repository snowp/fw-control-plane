package source.com.snowp.fw_control_plane;

import com.beust.jcommander.JCommander;
import io.envoyproxy.controlplane.cache.SimpleCache;
import io.envoyproxy.controlplane.server.DiscoveryServer;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import source.com.snowp.fw_control_plane.cli.Args;

public class Main {
  public static void main(String[] argv) throws Exception {

    Args args = new Args();
    JCommander.newBuilder()
        .args(argv)
        .addObject(args)
        .build();

    SimpleCache<String> cache = new SimpleCache<>(g -> "foo");

    Path configDirectory = Paths.get(args.configDir);

    // cheap and dirty way of ensuring we bump the version
    AtomicLong counter = new AtomicLong();

    FileConfigurationManager fileConfigurationManager =
        new FileConfigurationManager(configDirectory,
            ((group, resources) -> cache.setSnapshot(group,
                Snapshots.fromResourceMap(resources, ((Long) counter.incrementAndGet()).toString()))),
            new ResourceFileLoader(),
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

    Server grpcServer = NettyServerBuilder.forPort(args.port)
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
