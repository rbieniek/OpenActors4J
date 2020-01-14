package io.openactors4j.core.typed;

import io.openactors4j.core.common.ActorContext;
import io.openactors4j.core.common.Signal;

public interface ActorBehavior<T> {
  void setup(ActorContext context);

  ActorBehavior<T> receiveMessage(T message);

  ActorBehavior<T> receiveSignal(Signal signal);
}
