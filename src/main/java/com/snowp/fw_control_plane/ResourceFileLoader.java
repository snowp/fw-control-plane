package source.com.snowp.fw_control_plane;

import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads a resource file containing a json serialized proto message.
 */
public class ResourceFileLoader {

  private static final Logger logger = Logger.getLogger(ResourceFileLoader.class.getName());

  /**
   * Loads a proto message from a file.
   *
   * @param path path to file
   * @param defaultInstance instance of desired proto type
   * @param <T> message type
   * @return deserialized message if successful
   */
  <T extends Message> Optional<T> loadResource(Path path, T defaultInstance) {
    FileInputStream fileInputStream;
    Message.Builder resourceBuilder = defaultInstance.toBuilder();

    try {
      fileInputStream = new FileInputStream(path.toFile());
      JsonFormat.parser()
          .ignoringUnknownFields()
          .merge(new InputStreamReader(fileInputStream), resourceBuilder);
    } catch (IOException e) {
      logger.log(Level.WARNING, "failed to parse json at path=" + path.toString(), e);

      return Optional.empty();
    }

    //noinspection unchecked
    return Optional.of((T)resourceBuilder.build());
  }
}
