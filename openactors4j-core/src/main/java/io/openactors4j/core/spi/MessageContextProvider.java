package io.openactors4j.core.spi;

import java.util.function.Consumer;

public interface MessageContextProvider {
  void provideMessageContet(Consumer<Object> contextObjectConsumer);
}
