package source.com.snowp.fw_control_plane;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchKey;
import java.nio.file.WatchEvent;
import java.nio.file.WatchService;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import io.envoyproxy.controlplane.cache.Snapshot;
import io.envoyproxy.controlplane.cache.SimpleCache;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

public class Main {
  public static void main(String[] args) throws Exception {

    Path configDirectory = Paths.get(System.getProperty("user.dir"), args[0].split("/"));

    FileConfigurationManager fileConfigurationManager =
        new FileConfigurationManager(configDirectory,
            ((group, resources) -> System.out.println("group=" + group)), new ResourceFileReader(),
            Executors.newSingleThreadExecutor());

    WatchService watcher = FileSystems.getDefault().newWatchService();
    // watch the top level directory for changes in the set of groups 
    configDirectory.register(watcher, ENTRY_CREATE, ENTRY_DELETE);

    SimpleCache cache = new SimpleCache(null, g -> "foo");

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

    fwThread.start();
    fwThread.join();
  }
}
