package io.openactors4j.core.impl.actors;

import io.openactors4j.core.common.ActorContext;
import io.openactors4j.core.common.UnboundedMailbox;
import io.openactors4j.core.untyped.UntypedActor;

public class RootGuardian implements UntypedActor {
  private ActorContext actorContext;

  @Override
  public void onPreStart() {
    this.actorContext.spawnUntypedActor()
        .withActorInstance(new SystemGuardian())
        .withAbsoluteName("system")
        .withMailbox(new UnboundedMailbox<>())
        .create();

    this.actorContext.spawnUntypedActor()
        .withActorInstance(new UserGuardian())
        .withAbsoluteName("user")
        .withMailbox(new UnboundedMailbox<>())
        .create();
  }

  @Override
  public void setContext(final ActorContext context) {
    this.actorContext = actorContext;
  }

  @Override
  public void receive(final Object message) {

  }
}
