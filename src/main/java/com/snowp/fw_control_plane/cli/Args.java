package source.com.snowp.fw_control_plane.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

public class Args {
  @Parameter(names = "-config-dir", description = "folder to read config files from", required = true)
  public String configDir;

  @Parameter(names = "-port", description = "which port the grpc servers should listen to")
  public Integer port = 5005;
}
