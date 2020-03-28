package io.openactors4j.core.impl.common;

import static java.time.Duration.of;
import static java.util.EnumSet.allOf;
import static java.util.EnumSet.complementOf;
import static java.util.EnumSet.of;
import static java.util.Optional.empty;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


import io.openactors4j.core.common.Signal;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.junit.jupiter.api.Test;

@SuppressWarnings( {"PMD.TooManyMethods", "TooManyStaticImports"})
public class DelayedRestartSupervisionStrategyTest {
  private final ActorInstanceContext actorInstanceContext = mock(ActorInstanceContext.class);
  private final ExecutorService timerExecutorService = new ScheduledThreadPoolExecutor(16);

  @Test
  public void shouldHandleMessageProcessingExceptionWithoutMaxRetries() throws InterruptedException {
    final ActorInstanceStateTransition transition = mock(ActorInstanceStateTransition.class);
    final DelayedRestartSupervisionStrategy strategy = new DelayedRestartSupervisionStrategy(0,
        of(1, ChronoUnit.SECONDS),
        empty(),
        empty(), timerExecutorService);

    strategy.handleMessageProcessingException(new Exception(),
        transition,
        actorInstanceContext);

    allOf(InstanceState.class)
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(1))
        .transitionState(InstanceState.RESTARTING);
    complementOf(of(InstanceState.RESTARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    strategy.handleMessageProcessingException(new Exception(),
        transition,
        actorInstanceContext);

    verify(transition, times(1))
        .transitionState(InstanceState.RESTARTING);
    complementOf(of(InstanceState.RESTARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(2))
        .transitionState(InstanceState.RESTARTING);
    complementOf(of(InstanceState.RESTARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));
  }

  @Test
  public void shouldHandleActorCreationExceptionWithoutMaxRetries() throws InterruptedException {
    final ActorInstanceStateTransition transition = mock(ActorInstanceStateTransition.class);
    final DelayedRestartSupervisionStrategy strategy = new DelayedRestartSupervisionStrategy(0,
        of(1, ChronoUnit.SECONDS),
        empty(),
        empty(), timerExecutorService);

    strategy.handleActorCreationException(new Exception(),
        transition,
        actorInstanceContext);

    allOf(InstanceState.class)
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(1))
        .transitionState(InstanceState.CREATING);
    complementOf(of(InstanceState.CREATING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    strategy.handleActorCreationException(new Exception(),
        transition,
        actorInstanceContext);

    verify(transition, times(1))
        .transitionState(InstanceState.CREATING);
    complementOf(of(InstanceState.CREATING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(2))
        .transitionState(InstanceState.CREATING);
    complementOf(of(InstanceState.CREATING))
        .forEach(state -> verify(transition, never()).transitionState(state));
  }

  @Test
  public void shouldHandlePreStartSignalProcessingExceptionWithoutMaxRetries() throws InterruptedException {
    final ActorInstanceStateTransition transition = mock(ActorInstanceStateTransition.class);
    final DelayedRestartSupervisionStrategy strategy = new DelayedRestartSupervisionStrategy(0,
        of(1, ChronoUnit.SECONDS),
        empty(),
        empty(), timerExecutorService);

    strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_START,
        transition,
        actorInstanceContext);

    allOf(InstanceState.class)
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(1))
        .transitionState(InstanceState.STARTING);
    complementOf(of(InstanceState.STARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_START,
        transition,
        actorInstanceContext);

    verify(transition, times(1))
        .transitionState(InstanceState.STARTING);
    complementOf(of(InstanceState.STARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(2))
        .transitionState(InstanceState.STARTING);
    complementOf(of(InstanceState.STARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));
  }

  @Test
  public void shouldHandlePreRestartSignalProcessingExceptionWithoutMaxRetries() throws InterruptedException {
    final ActorInstanceStateTransition transition = mock(ActorInstanceStateTransition.class);
    final DelayedRestartSupervisionStrategy strategy = new DelayedRestartSupervisionStrategy(0,
        of(1, ChronoUnit.SECONDS),
        empty(),
        empty(), timerExecutorService);

    strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_RESTART,
        transition,
        actorInstanceContext);

    allOf(InstanceState.class)
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(1))
        .transitionState(InstanceState.STARTING);
    complementOf(of(InstanceState.STARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_RESTART,
        transition,
        actorInstanceContext);

    verify(transition, times(1))
        .transitionState(InstanceState.STARTING);
    complementOf(of(InstanceState.STARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(2))
        .transitionState(InstanceState.STARTING);
    complementOf(of(InstanceState.STARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));

  }

  @Test
  public void shouldHandleMessageProcessingExceptionWithtMaxRetries() throws InterruptedException {
    final ActorInstanceStateTransition transition = mock(ActorInstanceStateTransition.class);
    final DelayedRestartSupervisionStrategy strategy = new DelayedRestartSupervisionStrategy(1,
        of(1, ChronoUnit.SECONDS),
        empty(),
        empty(), timerExecutorService);

    strategy.handleMessageProcessingException(new Exception(),
        transition,
        actorInstanceContext);

    allOf(InstanceState.class)
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(1))
        .transitionState(InstanceState.RESTARTING);
    complementOf(of(InstanceState.RESTARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    strategy.handleMessageProcessingException(new Exception(),
        transition,
        actorInstanceContext);

    verify(transition, times(1))
        .transitionState(InstanceState.RESTARTING);
    verify(transition, times(1))
        .transitionState(InstanceState.STOPPING);
    complementOf(of(InstanceState.RESTARTING, InstanceState.STOPPING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(1))
        .transitionState(InstanceState.STOPPING);
    complementOf(of(InstanceState.RESTARTING, InstanceState.STOPPING))
        .forEach(state -> verify(transition, never()).transitionState(state));
  }

  @Test
  public void shouldHandleActorCreationExceptionWithtMaxRetries() throws InterruptedException {
    final ActorInstanceStateTransition transition = mock(ActorInstanceStateTransition.class);
    final DelayedRestartSupervisionStrategy strategy = new DelayedRestartSupervisionStrategy(1,
        of(1, ChronoUnit.SECONDS),
        empty(),
        empty(), timerExecutorService);

    strategy.handleActorCreationException(new Exception(),
        transition,
        actorInstanceContext);

    allOf(InstanceState.class)
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(1))
        .transitionState(InstanceState.CREATING);
    complementOf(of(InstanceState.CREATING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    strategy.handleActorCreationException(new Exception(),
        transition,
        actorInstanceContext);

    verify(transition, times(1))
        .transitionState(InstanceState.CREATING);
    verify(transition, times(1))
        .transitionState(InstanceState.STOPPED);
    complementOf(of(InstanceState.CREATING, InstanceState.STOPPED))
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(1))
        .transitionState(InstanceState.CREATING);
    verify(transition, times(1))
        .transitionState(InstanceState.STOPPED);
    complementOf(of(InstanceState.CREATING, InstanceState.STOPPED))
        .forEach(state -> verify(transition, never()).transitionState(state));
  }

  @Test
  public void shouldHandlePreStartSignalProcessingExceptionWithMaxRetries() throws InterruptedException {
    final ActorInstanceStateTransition transition = mock(ActorInstanceStateTransition.class);
    final DelayedRestartSupervisionStrategy strategy = new DelayedRestartSupervisionStrategy(1,
        of(1, ChronoUnit.SECONDS),
        empty(),
        empty(), timerExecutorService);

    strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_START,
        transition,
        actorInstanceContext);

    allOf(InstanceState.class)
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(1))
        .transitionState(InstanceState.STARTING);
    complementOf(of(InstanceState.STARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_START,
        transition,
        actorInstanceContext);

    verify(transition, times(1))
        .transitionState(InstanceState.STARTING);
    verify(transition, times(1))
        .transitionState(InstanceState.STOPPING);
    complementOf(of(InstanceState.STARTING, InstanceState.STOPPING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(1))
        .transitionState(InstanceState.STARTING);
    verify(transition, times(1))
        .transitionState(InstanceState.STOPPING);
    complementOf(of(InstanceState.STARTING, InstanceState.STOPPING))
        .forEach(state -> verify(transition, never()).transitionState(state));
  }

  @Test
  public void shouldHandlePreRestartSignalProcessingExceptionWithMaxRetries() throws InterruptedException {
    final ActorInstanceStateTransition transition = mock(ActorInstanceStateTransition.class);
    final DelayedRestartSupervisionStrategy strategy = new DelayedRestartSupervisionStrategy(1,
        of(1, ChronoUnit.SECONDS),
        empty(),
        empty(), timerExecutorService);

    strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_RESTART,
        transition,
        actorInstanceContext);

    allOf(InstanceState.class)
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(1))
        .transitionState(InstanceState.RESTARTING);
    complementOf(of(InstanceState.RESTARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_RESTART,
        transition,
        actorInstanceContext);

    verify(transition, times(1))
        .transitionState(InstanceState.RESTARTING);
    verify(transition, times(1))
        .transitionState(InstanceState.STOPPING);
    complementOf(of(InstanceState.RESTARTING, InstanceState.STOPPING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(1))
        .transitionState(InstanceState.RESTARTING);
    verify(transition, times(1))
        .transitionState(InstanceState.STOPPING);
    complementOf(of(InstanceState.RESTARTING, InstanceState.STOPPING))
        .forEach(state -> verify(transition, never()).transitionState(state));
  }

  @Test
  public void shouldHandleMessageProcessingExceptionWithoutMaxRetriesWithBackoff() throws InterruptedException {
    final ActorInstanceStateTransition transition = mock(ActorInstanceStateTransition.class);
    final DelayedRestartSupervisionStrategy strategy = new DelayedRestartSupervisionStrategy(0,
        of(1, ChronoUnit.SECONDS),
        Optional.of(of(1, ChronoUnit.SECONDS)),
        empty(),
        timerExecutorService);

    strategy.handleMessageProcessingException(new Exception(),
        transition,
        actorInstanceContext);

    allOf(InstanceState.class)
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(1))
        .transitionState(InstanceState.RESTARTING);
    complementOf(of(InstanceState.RESTARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    strategy.handleMessageProcessingException(new Exception(),
        transition,
        actorInstanceContext);

    verify(transition, times(1))
        .transitionState(InstanceState.RESTARTING);
    complementOf(of(InstanceState.RESTARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(1))
        .transitionState(InstanceState.RESTARTING);
    complementOf(of(InstanceState.RESTARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(2))
        .transitionState(InstanceState.RESTARTING);
    complementOf(of(InstanceState.RESTARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));
  }

  @Test
  public void shouldHandleActorCreationExceptionWithoutMaxRetriesWithBackoff() throws InterruptedException {
    final ActorInstanceStateTransition transition = mock(ActorInstanceStateTransition.class);
    final DelayedRestartSupervisionStrategy strategy = new DelayedRestartSupervisionStrategy(0,
        of(1, ChronoUnit.SECONDS),
        Optional.of(of(1, ChronoUnit.SECONDS)),
        empty(),
        timerExecutorService);

    strategy.handleActorCreationException(new Exception(),
        transition,
        actorInstanceContext);

    allOf(InstanceState.class)
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(1))
        .transitionState(InstanceState.CREATING);
    complementOf(of(InstanceState.CREATING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    strategy.handleActorCreationException(new Exception(),
        transition,
        actorInstanceContext);

    verify(transition, times(1))
        .transitionState(InstanceState.CREATING);
    complementOf(of(InstanceState.CREATING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(1))
        .transitionState(InstanceState.CREATING);
    complementOf(of(InstanceState.CREATING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(2))
        .transitionState(InstanceState.CREATING);
    complementOf(of(InstanceState.CREATING))
        .forEach(state -> verify(transition, never()).transitionState(state));
  }

  @Test
  public void shouldHandlePreStartSignalProcessingExceptionWithoutMaxRetriesWithBackoff() throws InterruptedException {
    final ActorInstanceStateTransition transition = mock(ActorInstanceStateTransition.class);
    final DelayedRestartSupervisionStrategy strategy = new DelayedRestartSupervisionStrategy(0,
        of(1, ChronoUnit.SECONDS),
        Optional.of(of(1, ChronoUnit.SECONDS)),
        empty(),
        timerExecutorService);

    strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_START,
        transition,
        actorInstanceContext);

    allOf(InstanceState.class)
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(1))
        .transitionState(InstanceState.STARTING);
    complementOf(of(InstanceState.STARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_START,
        transition,
        actorInstanceContext);

    verify(transition, times(1))
        .transitionState(InstanceState.STARTING);
    complementOf(of(InstanceState.STARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(1))
        .transitionState(InstanceState.STARTING);
    complementOf(of(InstanceState.STARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(2))
        .transitionState(InstanceState.STARTING);
    complementOf(of(InstanceState.STARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));
  }

  @Test
  public void shouldHandlePreRestartSignalProcessingExceptionWithoutMaxRetriesWithBackoff() throws InterruptedException {
    final ActorInstanceStateTransition transition = mock(ActorInstanceStateTransition.class);
    final DelayedRestartSupervisionStrategy strategy = new DelayedRestartSupervisionStrategy(0,
        of(1, ChronoUnit.SECONDS),
        Optional.of(of(1, ChronoUnit.SECONDS)),
        empty(),
        timerExecutorService);

    strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_RESTART,
        transition,
        actorInstanceContext);

    allOf(InstanceState.class)
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(1))
        .transitionState(InstanceState.RESTARTING);
    complementOf(of(InstanceState.RESTARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_RESTART,
        transition,
        actorInstanceContext);

    verify(transition, times(1))
        .transitionState(InstanceState.RESTARTING);
    complementOf(of(InstanceState.RESTARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(1))
        .transitionState(InstanceState.RESTARTING);
    complementOf(of(InstanceState.RESTARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(2))
        .transitionState(InstanceState.RESTARTING);
    complementOf(of(InstanceState.RESTARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));
  }

  @Test
  public void shouldHandleMessageProcessingExceptionWithoutMaxRetriesWithBackoffWithFactor() throws InterruptedException {
    final ActorInstanceStateTransition transition = mock(ActorInstanceStateTransition.class);
    final DelayedRestartSupervisionStrategy strategy = new DelayedRestartSupervisionStrategy(0,
        of(1, ChronoUnit.SECONDS),
        Optional.of(of(1, ChronoUnit.SECONDS)),
        Optional.of(1),
        timerExecutorService);

    strategy.handleMessageProcessingException(new Exception(),
        transition,
        actorInstanceContext);

    allOf(InstanceState.class)
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(1))
        .transitionState(InstanceState.RESTARTING);
    complementOf(of(InstanceState.RESTARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    strategy.handleMessageProcessingException(new Exception(),
        transition,
        actorInstanceContext);

    Thread.sleep(1100L);

    verify(transition, times(1))
        .transitionState(InstanceState.RESTARTING);
    complementOf(of(InstanceState.RESTARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(2))
        .transitionState(InstanceState.RESTARTING);
    complementOf(of(InstanceState.RESTARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    strategy.handleMessageProcessingException(new Exception(),
        transition,
        actorInstanceContext);

    Thread.sleep(1100L);

    verify(transition, times(2))
        .transitionState(InstanceState.RESTARTING);
    complementOf(of(InstanceState.RESTARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(2))
        .transitionState(InstanceState.RESTARTING);
    complementOf(of(InstanceState.RESTARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(3))
        .transitionState(InstanceState.RESTARTING);
    complementOf(of(InstanceState.RESTARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));
  }

  @Test
  public void shouldHandleActorCreationExceptionWithoutMaxRetriesWithBackoffWithFactor() throws InterruptedException {
    final ActorInstanceStateTransition transition = mock(ActorInstanceStateTransition.class);
    final DelayedRestartSupervisionStrategy strategy = new DelayedRestartSupervisionStrategy(0,
        of(1, ChronoUnit.SECONDS),
        Optional.of(of(1, ChronoUnit.SECONDS)),
        Optional.of(1),
        timerExecutorService);

    strategy.handleActorCreationException(new Exception(),
        transition,
        actorInstanceContext);

    allOf(InstanceState.class)
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(1))
        .transitionState(InstanceState.CREATING);
    complementOf(of(InstanceState.CREATING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    strategy.handleActorCreationException(new Exception(),
        transition,
        actorInstanceContext);

    Thread.sleep(1100L);

    verify(transition, times(1))
        .transitionState(InstanceState.CREATING);
    complementOf(of(InstanceState.CREATING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(2))
        .transitionState(InstanceState.CREATING);
    complementOf(of(InstanceState.CREATING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    strategy.handleActorCreationException(new Exception(),
        transition,
        actorInstanceContext);

    Thread.sleep(1100L);

    verify(transition, times(2))
        .transitionState(InstanceState.CREATING);
    complementOf(of(InstanceState.CREATING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(2))
        .transitionState(InstanceState.CREATING);
    complementOf(of(InstanceState.CREATING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(3))
        .transitionState(InstanceState.CREATING);
    complementOf(of(InstanceState.CREATING))
        .forEach(state -> verify(transition, never()).transitionState(state));
  }

  @Test
  public void shouldHandlePreStartSignalProcessingExceptionWithoutMaxRetriesWithBackoffWithFactor() throws InterruptedException {
    final ActorInstanceStateTransition transition = mock(ActorInstanceStateTransition.class);
    final DelayedRestartSupervisionStrategy strategy = new DelayedRestartSupervisionStrategy(0,
        of(1, ChronoUnit.SECONDS),
        Optional.of(of(1, ChronoUnit.SECONDS)),
        Optional.of(1),
        timerExecutorService);

    strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_START,
        transition,
        actorInstanceContext);

    allOf(InstanceState.class)
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(1))
        .transitionState(InstanceState.STARTING);
    complementOf(of(InstanceState.STARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_START,
        transition,
        actorInstanceContext);

    Thread.sleep(1100L);

    verify(transition, times(1))
        .transitionState(InstanceState.STARTING);
    complementOf(of(InstanceState.STARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(2))
        .transitionState(InstanceState.STARTING);
    complementOf(of(InstanceState.STARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_START,
        transition,
        actorInstanceContext);

    Thread.sleep(1100L);

    verify(transition, times(2))
        .transitionState(InstanceState.STARTING);
    complementOf(of(InstanceState.STARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(2))
        .transitionState(InstanceState.STARTING);
    complementOf(of(InstanceState.STARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(3))
        .transitionState(InstanceState.STARTING);
    complementOf(of(InstanceState.STARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));
  }

  @Test
  public void shouldHandlePreRestartSignalProcessingExceptionWithoutMaxRetriesWithBackoffWithFactor() throws InterruptedException {
    final ActorInstanceStateTransition transition = mock(ActorInstanceStateTransition.class);
    final DelayedRestartSupervisionStrategy strategy = new DelayedRestartSupervisionStrategy(0,
        of(1, ChronoUnit.SECONDS),
        Optional.of(of(1, ChronoUnit.SECONDS)),
        Optional.of(1),
        timerExecutorService);

    strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_RESTART,
        transition,
        actorInstanceContext);

    allOf(InstanceState.class)
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(1))
        .transitionState(InstanceState.RESTARTING);
    complementOf(of(InstanceState.RESTARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_RESTART,
        transition,
        actorInstanceContext);

    Thread.sleep(1100L);

    verify(transition, times(1))
        .transitionState(InstanceState.RESTARTING);
    complementOf(of(InstanceState.RESTARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(2))
        .transitionState(InstanceState.RESTARTING);
    complementOf(of(InstanceState.RESTARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_RESTART,
        transition,
        actorInstanceContext);

    Thread.sleep(1100L);

    verify(transition, times(2))
        .transitionState(InstanceState.RESTARTING);
    complementOf(of(InstanceState.RESTARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(2))
        .transitionState(InstanceState.RESTARTING);
    complementOf(of(InstanceState.RESTARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));

    Thread.sleep(1100L);

    verify(transition, times(3))
        .transitionState(InstanceState.RESTARTING);
    complementOf(of(InstanceState.RESTARTING))
        .forEach(state -> verify(transition, never()).transitionState(state));
  }
}
