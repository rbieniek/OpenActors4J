package io.openactors4j.core.impl.common;

import io.openactors4j.core.common.SystemAddress;
import java.net.URI;
import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;

@Data
@Builder
public class SystemAddressImpl implements SystemAddress {
  private String transportScheme;
  private String systemName;
  private String hostname;
  private int port;
  private String path;

  @Override
  @SneakyThrows
  public URI transport() {
    return new URI(new StringBuilder("actors.")
        .append(transportScheme)
        .append('.')
        .append(systemName)
        .toString(),
        null, hostname, port, path, null, null);
  }
}
