package io.openactors4j.core.impl.messaging;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.splitByWholeSeparatorPreserveAllTokens;
import static org.apache.commons.lang3.StringUtils.startsWith;


import io.openactors4j.core.common.SystemAddress;
import java.net.URI;
import java.util.Arrays;
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

  public SystemAddress validate() {
    if (isBlank(transportScheme)) {
      throw new IllegalArgumentException("Empty transport scheme detected");
    }
    if (isBlank(systemName)) {
      throw new IllegalArgumentException("Empty system name detected");
    }

    if (isBlank(hostname)) {
      throw new IllegalArgumentException("Empty host name detected");
    }

    if (!startsWith(path, "/")) {
      throw new IllegalArgumentException("Mailformed path detected");
    }


    if (Arrays.stream(splitByWholeSeparatorPreserveAllTokens(path, "/")).filter(s -> isBlank(s)).count() > 1) {
      throw new IllegalArgumentException("Empty path component detected");
    }

    return this;
  }

  @Override
  @SneakyThrows
  public URI transport() {
    return new URI(new StringBuilder(AddressConstants.URI_SCHEME_PREFIX)
        .append(transportScheme)
        .append('.')
        .append(systemName)
        .toString(),
        null, hostname, port, path, null, null);
  }
}
