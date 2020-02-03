package io.openactors4j.core.impl.messaging;

import static java.util.Collections.unmodifiableList;
import static org.apache.commons.lang3.StringUtils.isBlank;


import io.openactors4j.core.common.SystemAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * This class contains the complete routing slip of a message passed through the system
 */
public class RoutingSlip {

  @Getter
  private SystemAddress address;

  @Getter
  private String transport;
  @Getter
  private String systemName;
  @Getter
  private String hostName;
  @Getter
  private List<String> path;

  private Iterator<String> current;

  /**
   * Copy constructor with iterator reset
   *
   * @param source the {@link RoutingSlip} to copy
   * @return a fresh instance of {@link RoutingSlip} with the path iterator reset to the first
   * part of the path name
   */
  public static RoutingSlip copy(final RoutingSlip source) {
    final RoutingSlip instance = new RoutingSlip();

    instance.address = source.getAddress();
    instance.transport = source.getTransport();
    instance.systemName = source.getSystemName();
    instance.hostName = source.getHostName();
    instance.path = unmodifiableList(new LinkedList<>(source.getPath()));

    instance.current = instance.path.iterator();
    instance.current.next();

    return instance;
  }

  private RoutingSlip() {}

  public RoutingSlip(final SystemAddress address) {
    this.address = address;

    final String[] schemeParts = address.transport().getScheme().split("\\.");

    if (schemeParts.length != 3) {
      throw new IllegalArgumentException("Invalid scheme: " + address.transport().getScheme());
    }

    if (isBlank(schemeParts[1]) || isBlank(schemeParts[2])) {
      throw new IllegalArgumentException("Invalid scheme: " + address.transport().getScheme());
    }

    this.transport = schemeParts[1];
    this.systemName = schemeParts[2];

    this.hostName = address.transport().getHost();

    if (isBlank(this.hostName)) {
      throw new IllegalArgumentException("Empty hostname");
    }

    processPath(address.transport().getPath());
  }

  public RoutingSlip(final String systemName, final String absolutePath) {
    this(SystemAddressImpl.builder()
        .hostname(AddressConstants.CURRENT_HOST)
        .path(absolutePath)
        .transportScheme(AddressConstants.TRANSPORT_SCHEME_LOCAL)
        .systemName(systemName)
        .build());
  }

  private void processPath(final String absolutePath) {
    final String[] pathParts = absolutePath.split("/");

    if (pathParts.length <= 2) {
      throw new IllegalArgumentException("Illegal path: " + address.transport().getPath());
    }

    path = unmodifiableList(Arrays.asList(pathParts));

    if (path.stream().filter(part -> StringUtils.isBlank(part)).count() > 1) {
      throw new IllegalArgumentException("Blank path component detected");
    }

    current = path.iterator();
    current.next();
  }

  public Optional<String> nextPathPart() {
    return Optional.of(current)
        .filter(it -> it.hasNext())
        .map(it -> it.next());
  }

  public boolean isChildPartAvailable() {
    return current.hasNext();
  }
}
