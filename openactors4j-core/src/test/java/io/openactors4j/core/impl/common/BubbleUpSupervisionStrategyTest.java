package io.openactors4j.core.impl.common;

import io.openactors4j.core.common.Signal;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class BubbleUpSupervisionStrategyTest {
  private final BubbleUpSupervisionStrategy strategy = new BubbleUpSupervisionStrategy();
  private final ActorInstanceContext actorInstanceContext = Mockito.mock(ActorInstanceContext.class);

  @Test
  public void shouldHandleMessageProcessingException() {
    final ActorInstanceStateTransition transition = Mockito.mock(ActorInstanceStateTransition.class);

    strategy.handleMessageProcessingException(new Exception(),
        transition,
        actorInstanceContext);

    Mockito.verify(transition, Mockito.never())
        .transitionState(Mockito.any(InstanceState.class));
  }

  @Test
  public void shouldHandleActorCreationException() {
    final ActorInstanceStateTransition transition = Mockito.mock(ActorInstanceStateTransition.class);

    strategy.handleMessageProcessingException(new Exception(),
        transition,
        actorInstanceContext);

    Mockito.verify(transition, Mockito.never())
        .transitionState(Mockito.any(InstanceState.class));
  }

  @Test
  public void shouldHandleSignalProcessingException() {
    final ActorInstanceStateTransition transition = Mockito.mock(ActorInstanceStateTransition.class);

    strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_START,
        transition,
        actorInstanceContext);

    Mockito.verify(transition, Mockito.never())
        .transitionState(Mockito.any(InstanceState.class));
  }
}
