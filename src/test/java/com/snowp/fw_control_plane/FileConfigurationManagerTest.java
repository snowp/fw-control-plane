package com.snowp.fw_control_plane;

import com.google.common.collect.Multimap;
import com.google.protobuf.Duration;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import envoy.api.v2.Cds;
import io.envoyproxy.controlplane.cache.Resources;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import source.com.snowp.fw_control_plane.FileConfigurationManager;
import source.com.snowp.fw_control_plane.ResourceFileReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FileConfigurationManagerTest {

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();
  private Path root;
  private AtomicReference<Multimap<String, Message>> latestResources =
      new AtomicReference<>();
  private ExecutorService executor;
  private FileConfigurationManager fileConfigurationManager;
  private static final Cds.Cluster CLUSTER_A = Cds.Cluster.newBuilder()
      .setConnectTimeout(Duration.newBuilder().setSeconds(1))
      .setName("cluster_a")
      .build();
  private static final Cds.Cluster CLUSTER_B = Cds.Cluster.newBuilder()
      .setName("cluster2")
      .build();

  @Before
  public void setUp() throws IOException {
    root = temporaryFolder.getRoot().toPath();
    executor = Executors.newSingleThreadExecutor();
    fileConfigurationManager =
        new FileConfigurationManager(root, (a, b) -> latestResources.set(b),
            new ResourceFileReader(), executor);
  }

  @Test
  public void testFileWatching() throws IOException, InterruptedException {
    newFile(CLUSTER_A, "cluster_a.json", "foo", "clusters");

    // this takes a little while
    while (latestResources.get() == null) {
      fileConfigurationManager.processFileEvents();
    }

    assertEquals(latestResources.get().get(Resources.CLUSTER_TYPE_URL).iterator().next(),
        CLUSTER_A);
  }

  @Test
  public void testInitialState() throws IOException, InterruptedException {

    ConcurrentHashMap<String, Multimap<String, Message>> seenConfigurations =
        new ConcurrentHashMap<>();

    AtomicBoolean duplicates = new AtomicBoolean();

    FileConfigurationManager.ResourceUpdateCallback resourceUpdateCallback =
        (group, resources) -> seenConfigurations.compute(group, (key, value) -> {
              if (value != null) {
                duplicates.set(true);
              }
              return resources;
            }
        );

    newFile(CLUSTER_A, "cluster_1.json", "foo", "clusters");
    newFile(CLUSTER_B, "cluster_2.json", "bar", "clusters");
    fileConfigurationManager =
        new FileConfigurationManager(root, resourceUpdateCallback, new ResourceFileReader(),
            executor);

    do {
      Thread.sleep(2000);
    } while (seenConfigurations.size() != 2);

    assertFalse(duplicates.get());
    assertTrue(seenConfigurations.get("foo").get(Resources.CLUSTER_TYPE_URL).contains(CLUSTER_A));
    assertTrue(seenConfigurations.get("bar").get(Resources.CLUSTER_TYPE_URL).contains(CLUSTER_B));
  }

  private void newFile(Message message, String filename, String... path) throws IOException {
    File clusterFolder = temporaryFolder.newFolder(path);

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(
        new File(clusterFolder, filename)))) {
      JsonFormat.printer().appendTo(message, writer);
    }
  }
}
