package io.openactors4j.core.impl.common;

import io.openactors4j.core.common.SystemAddress;
import java.util.Optional;
import java.util.Queue;
import lombok.Getter;

/**
 * This class contains the complete routing slip of a message passed through the system
 */
public class RoutingSlip {

  @Getter
  private String transport;
  @Getter
  private String systemName;
  @Getter
  private String hostName;
  @Getter
  private Queue<String> path;

  public RoutingSlip(final SystemAddress address) {

  }

  public Optional<String> currentPathPart() {
    return Optional.ofNullable(path.peek());
  }

  public Optional<String> popPathPart() {
    return Optional.ofNullable(path.poll());
  }

  public boolean havePathPart() {
    return !path.isEmpty();
  }
}
