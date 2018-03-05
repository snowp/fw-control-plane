package source.com.snowp.fw_control_plane;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
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

  @FunctionalInterface public interface ResourceUpdateCallback {
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
    watches.put("MAIN", configDirectory.register(watchService, ENTRY_CREATE, ENTRY_DELETE));

    try (DirectoryStream<Path> configDirectoryStream = Files.newDirectoryStream(configDirectory)) {
      for (Path group : configDirectoryStream) {
        System.out.println("group=" + group.getFileName().toString());
        // watch each group and keep track of the watches so we can cancel them
        String groupName = group.getFileName().toString();
        watches.put(groupName, group.register(watchService, ENTRY_CREATE, ENTRY_DELETE));

        HashMultimap<ResourceType, Message> resourcesMap = HashMultimap.create();

        try (DirectoryStream<Path> groupDirectoryStream = Files.newDirectoryStream(group)) {
          for (Path resource : groupDirectoryStream) {
            String resourceName = resource.getFileName().toString();

            watches.put(groupName + "/" + resourceName,
                resource.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY));

            ResourceType resourceType = resourceNames.get(resourceName);
            Iterable<Message> currentResources = currentResources(resource, resourceType);

            resourcesMap.putAll(resourceType, currentResources);
          }

          // todo: deterministly generate version from resources
          groupSnapshots.put(groupName, Snapshot.create(resourcesMap, "version"));

          ImmutableMultimap<ResourceType, Message> resourcesCopy =
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
              updateResource(groupName, resource.getFileName().toString());
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
        String resourceType = relativePath.getName(1).toString();

        // this has to be at the group level
        String group = relativePath.getName(0).toString();

        if (event.kind().equals(ENTRY_DELETE)) {
          clearResource(group, resourceType);
        } else {
          updateResource(group, resourceType);
        }
      }
    }
    key.reset();
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
      if (oldSnapshot == null) {
        // needs to be mutable becase we try to clean it out on the next few lines
        oldSnapshot = Snapshot.create(HashMultimap.create(), "first!");
      }
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
        resourceFileReader.readResource(path, resourceType).ifPresent(resources::add);
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
