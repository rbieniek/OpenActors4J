package io.openactors4j.core.impl.common;

import io.openactors4j.core.common.Signal;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class BubbleUpSupervisionStrategyTest {
  private final BubbleUpSupervisionStrategy strategy = new BubbleUpSupervisionStrategy();
  private final ActorInstance actorInstance = Mockito.mock(ActorInstance.class);
  private final ActorInstanceContext actorInstanceContext = Mockito.mock(ActorInstanceContext.class);

  @Test
  public void shouldHandleMessageProcessingException() {
    Assertions.assertThat(strategy.handleMessageProcessingException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .isEmpty();
  }

  @Test
  public void shouldHandleActorCreationException() {
    Assertions.assertThat(strategy.handleMessageProcessingException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .isEmpty();
  }

  @Test
  public void shouldHandleSignalProcessingException() {
    Assertions.assertThat(strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_START,
        actorInstance,
        actorInstanceContext))
        .isEmpty();
  }
}
