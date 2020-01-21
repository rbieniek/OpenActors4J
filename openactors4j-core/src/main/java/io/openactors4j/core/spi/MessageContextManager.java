package io.openactors4j.core.spi;

import java.util.Optional;

/**
 *
 */
public interface MessageContextManager {

  void wrapMessageContext(Optional<Object> context);

  void unwrapMessageContext(Optional<Object> context);
}
