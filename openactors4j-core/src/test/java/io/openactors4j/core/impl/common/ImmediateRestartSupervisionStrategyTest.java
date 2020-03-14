package io.openactors4j.core.impl.common;

import io.openactors4j.core.common.Signal;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ImmediateRestartSupervisionStrategyTest {
  private final ActorInstance actorInstance = Mockito.mock(ActorInstance.class);
  private final ActorInstanceContext actorInstanceContext = Mockito.mock(ActorInstanceContext.class);

  @Test
  public void shouldHandleMessageProcessingExceptionWithoutMaxRetries() {
    final ImmediateRestartSupervisionStrategy strategy = new ImmediateRestartSupervisionStrategy(0);

    Assertions.assertThat(strategy.handleMessageProcessingException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .isEqualTo(InstanceState.RUNNING);

    Assertions.assertThat(strategy.handleMessageProcessingException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .isEqualTo(InstanceState.RUNNING);
  }


  @Test
  public void shouldHandleSignalProcessingExceptionWithoutMaxRetries() {
    final ImmediateRestartSupervisionStrategy strategy = new ImmediateRestartSupervisionStrategy(0);

    Assertions.assertThat(strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_START,
        actorInstance,
        actorInstanceContext))
        .isEqualTo(InstanceState.RUNNING);

    Assertions.assertThat(strategy.handleMessageProcessingException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .isEqualTo(InstanceState.RUNNING);
  }

  @Test
  public void shouldHandleMessageProcessingExceptionWithtMaxRetries() {
    final ImmediateRestartSupervisionStrategy strategy = new ImmediateRestartSupervisionStrategy(1);

    Assertions.assertThat(strategy.handleMessageProcessingException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .isEqualTo(InstanceState.RUNNING);

    Assertions.assertThat(strategy.handleMessageProcessingException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .isEqualTo(InstanceState.STOPPED);
  }


  @Test
  public void shouldHandleSignalProcessingExceptionWithMaxRetries() {
    final ImmediateRestartSupervisionStrategy strategy = new ImmediateRestartSupervisionStrategy(1);

    Assertions.assertThat(strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_START,
        actorInstance,
        actorInstanceContext))
        .isEqualTo(InstanceState.RUNNING);

    Assertions.assertThat(strategy.handleMessageProcessingException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .isEqualTo(InstanceState.STOPPED);
  }
}
