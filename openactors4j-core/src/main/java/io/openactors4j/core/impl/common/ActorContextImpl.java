package io.openactors4j.core.impl.common;

import io.openactors4j.core.common.ActorContext;
import io.openactors4j.core.common.ActorRef;
import io.openactors4j.core.common.SupervisionStrategies;
import io.openactors4j.core.typed.Behavior;
import io.openactors4j.core.typed.BehaviorBuilder;
import io.openactors4j.core.untyped.UntypedActorBuilder;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public class ActorContextImpl implements ActorContext {

  private final ActorInstanceContext instanceContext;

  @Setter
  private ActorRef currentSender;

  @Override
  public <T> BehaviorBuilder<T> newBehaviorBuilder() {
    return instanceContext.newBehaviorBuilder();
  }

  @Override
  public <T> ActorRef<T> spawn(final Behavior<T> behavior, final String name) {
    return instanceContext.spawn(behavior, name);
  }

  @Override
  public UntypedActorBuilder newUntypedActorBuilder() {
    return instanceContext.newUntypedActorBuilder();
  }

  @Override
  public <T> Optional<ActorRef<T>> lookupActor(final String name) {
    return Optional.empty();
  }

  @Override
  public SupervisionStrategies supervisionStrategies() {
    return instanceContext.supervisionStrategies();
  }

  @Override
  public ActorRef sender() {
    return currentSender;
  }
}
