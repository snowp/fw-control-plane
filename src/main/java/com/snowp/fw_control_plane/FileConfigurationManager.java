package source.com.snowp.fw_control_plane;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.protobuf.Message;
import envoy.api.v2.Cds;
import envoy.api.v2.Eds;
import envoy.api.v2.Lds;
import envoy.api.v2.Rds;
import io.envoyproxy.controlplane.cache.Resources;
import io.envoyproxy.controlplane.cache.Snapshot;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

public class FileConfigurationManager {

  private final WatchService watchService = FileSystems.getDefault().newWatchService();
  private final Path configDirectory;
  private final ResourceUpdateCallback resourceUpdateCallback;
  private final ResourceFileReader resourceFileReader;
  private final ExecutorService callbackExecutor;

  private final Object monitor = new Object();

  private final Map<String, WatchKey> watches = new HashMap<>();
  private final Map<String, Snapshot> groupSnapshots = new HashMap<>();

  // Mapping from the short form of a resource name to the full type url
  private static final BiMap<String, String> shortFormToTypeUrl = ImmutableBiMap.of(
      "clusters", Resources.CLUSTER_TYPE_URL,
      "routes", Resources.ROUTE_TYPE_URL,
      "listeners", Resources.LISTENER_TYPE_URL,
      "endpoints", Resources.ENDPOINT_TYPE_URL
  );

  private static final Map<String, Message> defaultMessages = ImmutableMap.of(
      Resources.CLUSTER_TYPE_URL, Cds.Cluster.getDefaultInstance(),
      Resources.ROUTE_TYPE_URL, Rds.RouteConfiguration.getDefaultInstance(),
      Resources.LISTENER_TYPE_URL, Lds.Listener.getDefaultInstance(),
      Resources.ENDPOINT_TYPE_URL, Eds.ClusterLoadAssignment.getDefaultInstance()
  );

  @FunctionalInterface public interface ResourceUpdateCallback {
    void onResourceUpdate(String group, Multimap<String, Message> resources);
  }

  public FileConfigurationManager(Path configDirectory,
      ResourceUpdateCallback resourceUpdateCallback, ResourceFileReader resourceFileReader,
      ExecutorService callbackExecutor)
      throws IOException {
    this.configDirectory = configDirectory;
    this.resourceUpdateCallback = resourceUpdateCallback;
    this.resourceFileReader = resourceFileReader;
    this.callbackExecutor = callbackExecutor;

    initializeSnapshot();
  }

  // reads the current state of the file tree constructing an initial snapshot and setting
  // the appropriate watches
  private void initializeSnapshot() throws IOException {
    watches.put("MAIN", configDirectory.register(watchService, ENTRY_CREATE, ENTRY_DELETE));

    try (DirectoryStream<Path> configDirectoryStream = Files.newDirectoryStream(configDirectory)) {
      for (Path group : configDirectoryStream) {
        System.out.println("group=" + group.getFileName().toString());
        // watch each group and keep track of the watches so we can cancel them
        String groupName = group.getFileName().toString();
        watches.put(groupName, group.register(watchService, ENTRY_CREATE, ENTRY_DELETE));

        HashMultimap<String, Message> resourcesMap = HashMultimap.create();

        try (DirectoryStream<Path> groupDirectoryStream = Files.newDirectoryStream(group)) {
          for (Path resource : groupDirectoryStream) {
            String resourceName = resource.getFileName().toString();

            String typeUrl = shortFormToTypeUrl.get(resourceName);

            resourcesMap.putAll(typeUrl, currentResources(resource, defaultMessages.get(typeUrl)));

            watches.put(groupName + "/" + resourceName,
                resource.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY));
          }

          // todo: deterministly generate version from resources
          groupSnapshots.put(groupName, Snapshots.fromResourceMap(resourcesMap, "version"));

          ImmutableMultimap<String, Message> resourcesCopy =
              ImmutableMultimap.copyOf(resourcesMap);

          callbackExecutor.submit(
              () -> resourceUpdateCallback.onResourceUpdate(groupName, resourcesCopy));
        }
      }
    }
  }

  public void processFileEvents() throws InterruptedException, IOException {
    WatchKey key = watchService.poll(1, TimeUnit.SECONDS);

    if (key == null) {
      return;
    }

    for (WatchEvent<?> event : key.pollEvents()) {
      if (event.kind().equals(OVERFLOW)) {
        // in this case we should reset our watches and reload everything
        // because it means we've missed something due to the event buffer being full
        watches.clear();
        initializeSnapshot();
        return;
      }

      @SuppressWarnings("unchecked")
      WatchEvent<Path> pathWatchEvent = (WatchEvent<Path>) event;
      Path eventPath = pathWatchEvent.context();

      Path absolutePath = ((Path) key.watchable()).resolve(eventPath.toString());
      Path relativePath = configDirectory.relativize(absolutePath);
      if (relativePath.getNameCount() == 1) {
        // this is a group
        String groupName = eventPath.getFileName().toString();
        if (event.kind().equals(ENTRY_CREATE)) {
          // we got a new group, so parse the whole group directory and assign watches

          try (DirectoryStream<Path> resources = Files.newDirectoryStream(absolutePath)) {
            for (Path resource : resources) {
              updateResource(groupName, shortFormToTypeUrl.get(resource.getFileName().toString()));
            }
          }

          newGroupWatch(absolutePath, relativePath);
        } else if (event.kind().equals(ENTRY_DELETE)) {
          // this is gone, so cancel the watch
          watches.remove(groupName).cancel();
          groupSnapshots.remove(groupName);
          // todo(snowp): group delete callback
        } else {
          // UNREACHED: we don't register modify events for groups
        }
      } else if (relativePath.getNameCount() == 2) {
        // this is the addition or deletion of a resource
        // all we need to do is update the watch

        if (event.kind().equals(ENTRY_CREATE)) {
          watches.put(relativePath.toString(),
              absolutePath.register(watchService, ENTRY_CREATE, ENTRY_DELETE));
        } else {
          watches.remove(relativePath.toString()).cancel();
        }
      } else {
        // this means a resource file was modified, so let's read the entire resource directory
        String typeUrl = shortFormToTypeUrl.get(relativePath.getName(1).toString());

        // this has to be at the group level
        String group = relativePath.getName(0).toString();

        if (event.kind().equals(ENTRY_DELETE)) {
          clearResource(group, typeUrl);
        } else {
          updateResource(group, typeUrl);
        }
      }
    }
    key.reset();
  }

  private void clearResource(String group, String typeUrl) {
    synchronized (monitor) {
      Snapshot snapshot = groupSnapshots.get(group);
      Multimap<String, Message> resources = ImmutableMultimap.copyOf(Snapshots.toMultiMap(snapshot));
      resources.removeAll(typeUrl);

      groupSnapshots.put(group, Snapshots.fromResourceMap(resources, snapshot.version(Resources.CLUSTER_TYPE_URL)));
    }
  }

  private void updateResource(String group, String typeUrl) throws IOException {
    Iterable<? extends Message> resources =
        currentResources(configDirectory.resolve(group).resolve(shortFormToTypeUrl.inverse().get(typeUrl)),
            defaultMessages.get(typeUrl));

    synchronized (monitor) {
      Snapshot oldSnapshot = groupSnapshots.remove(group);
      if (oldSnapshot == null) {
        // needs to be mutable becase we try to clean it out on the next few lines
        oldSnapshot = Snapshot.create(Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(), "first!");
      }
      Multimap<String, Message> oldResourcesMap = Snapshots.toMultiMap(oldSnapshot);
      oldResourcesMap.removeAll(typeUrl);
      oldResourcesMap.putAll(typeUrl, resources);
      groupSnapshots.put(group, Snapshots.fromResourceMap(oldResourcesMap,
          oldSnapshot.version(Resources.CLUSTER_TYPE_URL)));

      ImmutableMultimap<String, Message> resourcesCopy =
          ImmutableMultimap.copyOf(oldResourcesMap);

      callbackExecutor.submit(() -> resourceUpdateCallback.onResourceUpdate(group, resourcesCopy));
    }
  }

  private <T extends Message> Iterable<T> currentResources(Path resourcePath, T defaultInstance)
      throws IOException {
    Set<T> resources = new HashSet<>();

    try (DirectoryStream<Path> pathDirectoryStream = Files.newDirectoryStream(resourcePath,
        "*.json")) {
      for (Path path : pathDirectoryStream) {
        resourceFileReader.readResource(path, defaultInstance).ifPresent(resources::add);
      }
    }

    return resources;
  }

  private void newGroupWatch(Path absolutePath, Path relativePath) {
    try {
      watches.put(relativePath.toString(),
          absolutePath.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
