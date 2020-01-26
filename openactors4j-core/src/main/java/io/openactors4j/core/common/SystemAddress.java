package io.openactors4j.core.common;

import java.net.URI;

public interface SystemAddress {
  URI transport();

  SystemAddress validate();
}
