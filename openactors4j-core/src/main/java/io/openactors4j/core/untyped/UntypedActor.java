package io.openactors4j.core.untyped;

import io.openactors4j.core.common.ActorContext;

public interface UntypedActor {
  void setContext(ActorContext context);

  void receive(Object message);
}
