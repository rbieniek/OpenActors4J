package io.openactors4j.core.impl.actors;

import io.openactors4j.core.common.ActorContext;
import io.openactors4j.core.common.UnboundedMailbox;
import io.openactors4j.core.impl.common.SystemActor;
import io.openactors4j.core.untyped.UntypedActor;

public class RootGuardian implements UntypedActor, SystemActor {
  private ActorContext actorContext;

  @Override
  public void onPreStart() {
    this.actorContext.newUntypedActorBuilder()
        .withSupplier(() -> new SystemGuardian())
        .withAbsoluteName("system")
        .withMailbox(new UnboundedMailbox<>())
        .withSupervisionStrategy(actorContext.supervisionStrategies().restart())
        .create();

    this.actorContext.newUntypedActorBuilder()
        .withSupplier(() -> new UserGuardian())
        .withAbsoluteName("user")
        .withMailbox(new UnboundedMailbox<>())
        .withSupervisionStrategy(actorContext.supervisionStrategies().restart())
        .create();
  }

  @Override
  public void setupContext(final ActorContext context) {
    this.actorContext = actorContext;
  }

  @Override
  public void receive(final Object message) {

  }
}
