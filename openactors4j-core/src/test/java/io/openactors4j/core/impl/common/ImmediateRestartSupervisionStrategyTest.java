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
        .hasValue(InstanceState.RESTARTING);

    Assertions.assertThat(strategy.handleMessageProcessingException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .hasValue(InstanceState.RESTARTING);
  }

  @Test
  public void shouldHandleActorCreationExceptionWithoutMaxRetries() {
    final ImmediateRestartSupervisionStrategy strategy = new ImmediateRestartSupervisionStrategy(0);

    Assertions.assertThat(strategy.handleActorCreationException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .hasValue(InstanceState.CREATING);

    Assertions.assertThat(strategy.handleActorCreationException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .hasValue(InstanceState.CREATING);
  }

  @Test
  public void shouldHandlePreStartSignalProcessingExceptionWithoutMaxRetries() {
    final ImmediateRestartSupervisionStrategy strategy = new ImmediateRestartSupervisionStrategy(0);

    Assertions.assertThat(strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_START,
        actorInstance,
        actorInstanceContext))
        .hasValue(InstanceState.STARTING);

    Assertions.assertThat(strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_START,
        actorInstance,
        actorInstanceContext))
        .hasValue(InstanceState.STARTING);
  }

  @Test
  public void shouldHandlePreRestartSignalProcessingExceptionWithoutMaxRetries() {
    final ImmediateRestartSupervisionStrategy strategy = new ImmediateRestartSupervisionStrategy(0);

    Assertions.assertThat(strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_RESTART,
        actorInstance,
        actorInstanceContext))
        .hasValue(InstanceState.RESTARTING);

    Assertions.assertThat(strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_RESTART,
        actorInstance,
        actorInstanceContext))
        .hasValue(InstanceState.RESTARTING);
  }

  @Test
  public void shouldHandleActorCreationgExceptionWithoutMaxRetries() {
    final ImmediateRestartSupervisionStrategy strategy = new ImmediateRestartSupervisionStrategy(0);

    Assertions.assertThat(strategy.handleActorCreationException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .hasValue(InstanceState.CREATING);

    Assertions.assertThat(strategy.handleActorCreationException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .hasValue(InstanceState.CREATING);
  }

  @Test
  public void shouldHandleMessageProcessingExceptionWithtMaxRetries() {
    final ImmediateRestartSupervisionStrategy strategy = new ImmediateRestartSupervisionStrategy(1);

    Assertions.assertThat(strategy.handleMessageProcessingException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .hasValue(InstanceState.RESTARTING);

    Assertions.assertThat(strategy.handleMessageProcessingException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .hasValue(InstanceState.STOPPING);
  }

  @Test
  public void shouldHandleActorCreationExceptionWithtMaxRetries() {
    final ImmediateRestartSupervisionStrategy strategy = new ImmediateRestartSupervisionStrategy(1);

    Assertions.assertThat(strategy.handleActorCreationException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .hasValue(InstanceState.CREATING);

    Assertions.assertThat(strategy.handleActorCreationException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .hasValue(InstanceState.STOPPED);
  }

  @Test
  public void shouldHandlePreStartSignalProcessingExceptionWithMaxRetries() {
    final ImmediateRestartSupervisionStrategy strategy = new ImmediateRestartSupervisionStrategy(1);

    Assertions.assertThat(strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_START,
        actorInstance,
        actorInstanceContext))
        .hasValue(InstanceState.STARTING);

    Assertions.assertThat(strategy.handleMessageProcessingException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .hasValue(InstanceState.STOPPED);
  }

  @Test
  public void shouldHandlePreRestartSignalProcessingExceptionWithMaxRetries() {
    final ImmediateRestartSupervisionStrategy strategy = new ImmediateRestartSupervisionStrategy(1);

    Assertions.assertThat(strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_RESTART,
        actorInstance,
        actorInstanceContext))
        .hasValue(InstanceState.RESTARTING);

    Assertions.assertThat(strategy.handleMessageProcessingException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .hasValue(InstanceState.STOPPED);
  }
}
