package io.openactors4j.core.typed;

import io.openactors4j.core.common.ActorContext;

public interface ActorBehavior<T> {
  void setContext(ActorContext context);

  ActorBehavior<T> receive(T message);
}
