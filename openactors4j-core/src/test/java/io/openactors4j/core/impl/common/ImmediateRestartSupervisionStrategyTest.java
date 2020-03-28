package io.openactors4j.core.impl.common;

import io.openactors4j.core.common.Signal;
import java.util.EnumSet;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ImmediateRestartSupervisionStrategyTest {
  private final ActorInstanceContext actorInstanceContext = Mockito.mock(ActorInstanceContext.class);

  @Test
  public void shouldHandleMessageProcessingExceptionWithoutMaxRetries() {
    final ImmediateRestartSupervisionStrategy strategy = new ImmediateRestartSupervisionStrategy(0);
    final ActorInstanceStateTransition transition = Mockito.mock(ActorInstanceStateTransition.class);

    strategy.handleMessageProcessingException(new Exception(),
        transition,
        actorInstanceContext);

    Mockito.verify(transition, Mockito.times(1))
        .transitionState(InstanceState.RESTARTING);
    EnumSet.complementOf(EnumSet.of(InstanceState.RESTARTING))
        .forEach(state -> Mockito.verify(transition, Mockito.never()).transitionState(state));

    strategy.handleMessageProcessingException(new Exception(),
        transition,
        actorInstanceContext);

    Mockito.verify(transition, Mockito.times(2))
        .transitionState(InstanceState.RESTARTING);
    EnumSet.complementOf(EnumSet.of(InstanceState.RESTARTING))
        .forEach(state -> Mockito.verify(transition, Mockito.never()).transitionState(state));
  }

  @Test
  public void shouldHandleActorCreationExceptionWithoutMaxRetries() {
    final ImmediateRestartSupervisionStrategy strategy = new ImmediateRestartSupervisionStrategy(0);
    final ActorInstanceStateTransition transition = Mockito.mock(ActorInstanceStateTransition.class);

    strategy.handleActorCreationException(new Exception(),
        transition,
        actorInstanceContext);

    Mockito.verify(transition, Mockito.times(1))
        .transitionState(InstanceState.CREATING);
    EnumSet.complementOf(EnumSet.of(InstanceState.CREATING))
        .forEach(state -> Mockito.verify(transition, Mockito.never()).transitionState(state));

    strategy.handleActorCreationException(new Exception(),
        transition,
        actorInstanceContext);

    Mockito.verify(transition, Mockito.times(2))
        .transitionState(InstanceState.CREATING);
    EnumSet.complementOf(EnumSet.of(InstanceState.CREATING))
        .forEach(state -> Mockito.verify(transition, Mockito.never()).transitionState(state));
  }

  @Test
  public void shouldHandlePreStartSignalProcessingExceptionWithoutMaxRetries() {
    final ImmediateRestartSupervisionStrategy strategy = new ImmediateRestartSupervisionStrategy(0);
    final ActorInstanceStateTransition transition = Mockito.mock(ActorInstanceStateTransition.class);

    strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_START,
        transition,
        actorInstanceContext);

    Mockito.verify(transition, Mockito.times(1))
        .transitionState(InstanceState.STARTING);
    EnumSet.complementOf(EnumSet.of(InstanceState.STARTING))
        .forEach(state -> Mockito.verify(transition, Mockito.never()).transitionState(state));

    strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_START,
        transition,
        actorInstanceContext);

    Mockito.verify(transition, Mockito.times(2))
        .transitionState(InstanceState.STARTING);
    EnumSet.complementOf(EnumSet.of(InstanceState.STARTING))
        .forEach(state -> Mockito.verify(transition, Mockito.never()).transitionState(state));
  }

  @Test
  public void shouldHandlePreRestartSignalProcessingExceptionWithoutMaxRetries() {
    final ImmediateRestartSupervisionStrategy strategy = new ImmediateRestartSupervisionStrategy(0);
    final ActorInstanceStateTransition transition = Mockito.mock(ActorInstanceStateTransition.class);

    strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_RESTART,
        transition,
        actorInstanceContext);

    Mockito.verify(transition, Mockito.times(1))
        .transitionState(InstanceState.RESTARTING);
    EnumSet.complementOf(EnumSet.of(InstanceState.RESTARTING))
        .forEach(state -> Mockito.verify(transition, Mockito.never()).transitionState(state));

    strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_RESTART,
        transition,
        actorInstanceContext);

    Mockito.verify(transition, Mockito.times(2))
        .transitionState(InstanceState.RESTARTING);
    EnumSet.complementOf(EnumSet.of(InstanceState.RESTARTING))
        .forEach(state -> Mockito.verify(transition, Mockito.never()).transitionState(state));
  }

  @Test
  public void shouldHandleMessageProcessingExceptionWithtMaxRetries() {
    final ImmediateRestartSupervisionStrategy strategy = new ImmediateRestartSupervisionStrategy(1);
    final ActorInstanceStateTransition transition = Mockito.mock(ActorInstanceStateTransition.class);

    strategy.handleMessageProcessingException(new Exception(),
        transition,
        actorInstanceContext);

    Mockito.verify(transition, Mockito.times(1))
        .transitionState(InstanceState.RESTARTING);
    EnumSet.complementOf(EnumSet.of(InstanceState.RESTARTING))
        .forEach(state -> Mockito.verify(transition, Mockito.never()).transitionState(state));

    strategy.handleMessageProcessingException(new Exception(),
        transition,
        actorInstanceContext);

    Mockito.verify(transition, Mockito.times(1))
        .transitionState(InstanceState.RESTARTING);
    Mockito.verify(transition, Mockito.times(1))
        .transitionState(InstanceState.STOPPING);
    EnumSet.complementOf(EnumSet.of(InstanceState.RESTARTING, InstanceState.STOPPING))
        .forEach(state -> Mockito.verify(transition, Mockito.never()).transitionState(state));

  }

  @Test
  public void shouldHandleActorCreationExceptionWithtMaxRetries() {
    final ImmediateRestartSupervisionStrategy strategy = new ImmediateRestartSupervisionStrategy(1);
    final ActorInstanceStateTransition transition = Mockito.mock(ActorInstanceStateTransition.class);

    strategy.handleActorCreationException(new Exception(),
        transition,
        actorInstanceContext);

    Mockito.verify(transition, Mockito.times(1))
        .transitionState(InstanceState.CREATING);
    EnumSet.complementOf(EnumSet.of(InstanceState.CREATING))
        .forEach(state -> Mockito.verify(transition, Mockito.never()).transitionState(state));

    strategy.handleActorCreationException(new Exception(),
        transition,
        actorInstanceContext);

    Mockito.verify(transition, Mockito.times(1))
        .transitionState(InstanceState.CREATING);
    Mockito.verify(transition, Mockito.times(1))
        .transitionState(InstanceState.STOPPED);
    EnumSet.complementOf(EnumSet.of(InstanceState.CREATING, InstanceState.STOPPED))
        .forEach(state -> Mockito.verify(transition, Mockito.never()).transitionState(state));
  }

  @Test
  public void shouldHandlePreStartSignalProcessingExceptionWithMaxRetries() {
    final ImmediateRestartSupervisionStrategy strategy = new ImmediateRestartSupervisionStrategy(1);
    final ActorInstanceStateTransition transition = Mockito.mock(ActorInstanceStateTransition.class);

    strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_START,
        transition,
        actorInstanceContext);

    Mockito.verify(transition, Mockito.times(1))
        .transitionState(InstanceState.STARTING);
    EnumSet.complementOf(EnumSet.of(InstanceState.STARTING))
        .forEach(state -> Mockito.verify(transition, Mockito.never()).transitionState(state));

    strategy.handleMessageProcessingException(new Exception(),
        transition,
        actorInstanceContext);

    Mockito.verify(transition, Mockito.times(1))
        .transitionState(InstanceState.STARTING);
    Mockito.verify(transition, Mockito.times(1))
        .transitionState(InstanceState.STOPPED);
    EnumSet.complementOf(EnumSet.of(InstanceState.STARTING, InstanceState.STOPPED))
        .forEach(state -> Mockito.verify(transition, Mockito.never()).transitionState(state));
  }

  @Test
  public void shouldHandlePreRestartSignalProcessingExceptionWithMaxRetries() {
    final ImmediateRestartSupervisionStrategy strategy = new ImmediateRestartSupervisionStrategy(1);
    final ActorInstanceStateTransition transition = Mockito.mock(ActorInstanceStateTransition.class);

    strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_RESTART,
        transition,
        actorInstanceContext);

    Mockito.verify(transition, Mockito.times(1))
        .transitionState(InstanceState.RESTARTING);
    EnumSet.complementOf(EnumSet.of(InstanceState.RESTARTING))
        .forEach(state -> Mockito.verify(transition, Mockito.never()).transitionState(state));

    strategy.handleMessageProcessingException(new Exception(),
        transition,
        actorInstanceContext);

    Mockito.verify(transition, Mockito.times(1))
        .transitionState(InstanceState.RESTARTING);
    Mockito.verify(transition, Mockito.times(1))
        .transitionState(InstanceState.STOPPED);
    EnumSet.complementOf(EnumSet.of(InstanceState.RESTARTING, InstanceState.STOPPED))
        .forEach(state -> Mockito.verify(transition, Mockito.never()).transitionState(state));

  }
}
