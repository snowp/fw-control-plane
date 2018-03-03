package source.com.snowp.fw_control_plane;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.protobuf.Message;
import io.envoyproxy.controlplane.cache.ResourceType;
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
  private final WatchService watcher = FileSystems.getDefault().newWatchService();
  private final Path configDirectory;
  private final ResourceUpdateCallback resourceUpdateCallback;
  private final ResourceFileReader resourceFileReader;
  private ExecutorService callbackExecutor;

  private final Object monitor = new Object();

  private final Map<String, WatchKey> watches = new HashMap<>();
  private final Map<String, Snapshot> groupSnapshots = new HashMap<>();

  private static final Map<String, ResourceType> resourceNames = ImmutableMap.of(
      "clusters", ResourceType.CLUSTER,
      "routes", ResourceType.ROUTE,
      "listeners", ResourceType.LISTENER,
      "endpoints", ResourceType.ENDPOINT
  );

  @FunctionalInterface interface ResourceUpdateCallback {
    void onResourceUpdate(String group, Multimap<ResourceType, Message> resources);
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
    watches.put("MAIN", configDirectory.register(watcher, ENTRY_DELETE, ENTRY_CREATE));

    try (DirectoryStream<Path> configDirectoryStream = Files.newDirectoryStream(configDirectory)) {
      for (Path group : configDirectoryStream) {
        System.out.println("group=" + group.getFileName().toString());
        // watch each group and keep track of the watches so we can cancel them
        String groupName = group.getFileName().toString();
        watches.put(groupName,
            group.register(watcher, ENTRY_CREATE, ENTRY_DELETE));

        HashMultimap<ResourceType, Message> resourcesMap = HashMultimap.create();

        try (DirectoryStream<Path> groupDirectoryStream = Files.newDirectoryStream(group)) {
          for (Path resource : groupDirectoryStream) {
            String resourceName = resource.getFileName().toString();
            watches.put(groupName + "/" + resourceName,
                resource.register(watcher, ENTRY_CREATE, ENTRY_DELETE));

            ResourceType resourceType = resourceNames.get(resourceName);
            Iterable<Message> currentResources = currentResources(resource, resourceType);

            resourcesMap.putAll(resourceType, currentResources);
          }

          // todo: deterministly generate version from resources
          groupSnapshots.put(groupName, Snapshot.create(resourcesMap, "version"));
        }
      }
    }
  }

  void processFileEvents() throws InterruptedException, IOException {
    WatchKey key = watcher.poll(1, TimeUnit.SECONDS);

    if (key == null) {
      return;
    }

    System.out.println("non-null");

    for (WatchEvent<?> event : key.pollEvents()) {
      if (event.kind().equals(OVERFLOW)) {
        // in this case we should reset our watches and reload everything
        // because it means we've missed something due to the event buffer being full
        synchronized (monitor) {
          watches.clear();
        }
        initializeSnapshot();
        return;
      }

      @SuppressWarnings("unchecked")
      WatchEvent<Path> pathWatchEvent = (WatchEvent<Path>) event;
      Path eventPath = pathWatchEvent.context();

      if (eventPath.getParent().equals(configDirectory)) {
        // this is a group
        if (event.kind().equals(ENTRY_CREATE)) {
          // we got a new group, so parse the whole directory and assign watches

          newGroupWatch(eventPath);
        } else if (event.kind().equals(ENTRY_DELETE)) {
          // this is gone, so cancel the watch
          WatchKey groupWatch = watches.remove(eventPath.getFileName().toString());
          groupWatch.cancel();
        } else {
          // UNREACHED: we don't register modify events for groups
        }
      } else {
        // this has to be at the group level
        String group = eventPath.getParent().getFileName().toString();

        // todo: proper error handling
        String resourceType = eventPath.getFileName().toString().split("\\.")[0];

        if (event.kind().equals(ENTRY_DELETE)) {
          clearResource(group, resourceType);
        } else {
          updateResource(group, resourceType);
        }
      }
    }
  }

  private void clearResource(String group, String resourceType) {
    synchronized (monitor) {
      Snapshot snapshot = groupSnapshots.get(group);
      Multimap<ResourceType, Message> resources = ImmutableMultimap.copyOf(snapshot.resources());
      resources.removeAll(resourceNames.get(resourceType));

      groupSnapshots.put(group, Snapshot.create(resources, snapshot.version()));
    }
  }

  private void updateResource(String group, String resourceType) throws IOException {
    Iterable<Message> resources =
        currentResources(configDirectory.resolve(group).resolve(resourceType),
            resourceNames.get(resourceType));

    synchronized (monitor) {
      Snapshot oldSnapshot = groupSnapshots.remove(group);
      Multimap<ResourceType, Message> oldResourcesMap = oldSnapshot.resources();
      oldResourcesMap.removeAll(resourceNames.get(resourceType));
      oldResourcesMap.putAll(resourceNames.get(resourceType), resources);
      groupSnapshots.put(group, Snapshot.create(oldResourcesMap, oldSnapshot.version()));

      ImmutableMultimap<ResourceType, Message> resourcesCopy =
          ImmutableMultimap.copyOf(oldResourcesMap);

      callbackExecutor.submit(() -> resourceUpdateCallback.onResourceUpdate(group, resourcesCopy));
    }
  }

  private Iterable<Message> currentResources(Path resourcePath, ResourceType resourceType)
      throws IOException {
    Set<Message> resources = new HashSet<>();

    try (DirectoryStream<Path> pathDirectoryStream = Files.newDirectoryStream(resourcePath,
        "*.json")) {
      for (Path path : pathDirectoryStream) {
        resources.add(resourceFileReader.readResource(path, resourceType));
      }
    }

    return resources;
  }

  private void newGroupWatch(Path eventPath) {
    try {
      watches.put(eventPath.getFileName().toString(),
          eventPath.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
