package io.openactors4j.core.impl.common;

import io.openactors4j.core.common.Signal;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class TerminateSupervisionStrategyTest {
  private final TerminateSupervisionStrategy strategy = new TerminateSupervisionStrategy();
  private final ActorInstance actorInstance = Mockito.mock(ActorInstance.class);
  private final ActorInstanceContext actorInstanceContext = Mockito.mock(ActorInstanceContext.class);

  @Test
  public void shouldHandleMessageProcessingException() {
    Assertions.assertThat(strategy.handleMessageProcessingException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .hasValue(InstanceState.STOPPING);
  }

  @Test
  public void shouldHandleSignalProcessingException() {
    Assertions.assertThat(strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_START,
        actorInstance,
        actorInstanceContext))
        .hasValue(InstanceState.STOPPING);
  }

  @Test
  public void shouldHandlActorCreationException() {
    Assertions.assertThat(strategy.handleActorCreationException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .hasValue(InstanceState.STOPPED);
  }
}
