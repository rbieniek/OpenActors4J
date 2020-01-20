package io.openactors4j.core.typed;

import io.openactors4j.core.common.ActorContext;
import io.openactors4j.core.common.Signal;

public interface Behavior<T> {
  default void setup(ActorContext context) {
  }

  Behavior<T> receiveMessage(T message);

  Behavior<T> receiveSignal(Signal signal);
}
