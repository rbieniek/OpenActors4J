package io.openactors4j.core.impl.common;

import io.openactors4j.core.common.Signal;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class DelayedRestartSupervisionStrategyTest {
  private final ActorInstance actorInstance = Mockito.mock(ActorInstance.class);
  private final ActorInstanceContext actorInstanceContext = Mockito.mock(ActorInstanceContext.class);
  private final ExecutorService timerExecutorService = new ScheduledThreadPoolExecutor(16);

  @Test
  public void shouldHandleMessageProcessingExceptionWithoutMaxRetries() throws InterruptedException {
    final DelayedRestartSupervisionStrategy strategy = new DelayedRestartSupervisionStrategy(0,
        Duration.of(1, ChronoUnit.SECONDS),
        Optional.empty(),
        Optional.empty(),timerExecutorService);

    Assertions.assertThat(strategy.handleMessageProcessingException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .isEmpty();

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.RESTARTING);

    Assertions.assertThat(strategy.handleMessageProcessingException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .isEmpty();

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(2))
        .transitionState(InstanceState.RESTARTING);
  }

  @Test
  public void shouldHandleActorCreationExceptionWithoutMaxRetries() throws InterruptedException {
    final DelayedRestartSupervisionStrategy strategy = new DelayedRestartSupervisionStrategy(0,
        Duration.of(1, ChronoUnit.SECONDS),
        Optional.empty(),
        Optional.empty(),timerExecutorService);

    Assertions.assertThat(strategy.handleActorCreationException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .isEmpty();

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.CREATING);

    Assertions.assertThat(strategy.handleActorCreationException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .isEmpty();

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(2))
        .transitionState(InstanceState.CREATING);
  }

  @Test
  public void shouldHandlePreStartSignalProcessingExceptionWithoutMaxRetries() throws InterruptedException {
    final DelayedRestartSupervisionStrategy strategy = new DelayedRestartSupervisionStrategy(0,
        Duration.of(1, ChronoUnit.SECONDS),
        Optional.empty(),
        Optional.empty(),timerExecutorService);

    Assertions.assertThat(strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_START,
        actorInstance,
        actorInstanceContext))
        .isEmpty();

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.STARTING);


    Assertions.assertThat(strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_START,
        actorInstance,
        actorInstanceContext))
        .isEmpty();

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(2))
        .transitionState(InstanceState.STARTING);
  }

  @Test
  public void shouldHandlePreRestartSignalProcessingExceptionWithoutMaxRetries() throws InterruptedException {
    final DelayedRestartSupervisionStrategy strategy = new DelayedRestartSupervisionStrategy(0,
        Duration.of(1, ChronoUnit.SECONDS),
        Optional.empty(),
        Optional.empty(),timerExecutorService);

    Assertions.assertThat(strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_RESTART,
        actorInstance,
        actorInstanceContext))
        .isEmpty();

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.STARTING);


    Assertions.assertThat(strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_RESTART,
        actorInstance,
        actorInstanceContext))
        .isEmpty();

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(2))
        .transitionState(InstanceState.STARTING);
  }

  @Test
  public void shouldHandleMessageProcessingExceptionWithtMaxRetries() throws InterruptedException {
    final DelayedRestartSupervisionStrategy strategy = new DelayedRestartSupervisionStrategy(1,
        Duration.of(1, ChronoUnit.SECONDS),
        Optional.empty(),
        Optional.empty(),timerExecutorService);

    Assertions.assertThat(strategy.handleMessageProcessingException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .isEmpty();

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.RESTARTING);

    Assertions.assertThat(strategy.handleMessageProcessingException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .hasValue(InstanceState.STOPPING);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.RESTARTING);
  }

  @Test
  public void shouldHandleActorCreationExceptionWithtMaxRetries() throws InterruptedException {
    final DelayedRestartSupervisionStrategy strategy = new DelayedRestartSupervisionStrategy(1,
        Duration.of(1, ChronoUnit.SECONDS),
        Optional.empty(),
        Optional.empty(),timerExecutorService);

    Assertions.assertThat(strategy.handleActorCreationException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .isEmpty();

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.CREATING);

    Assertions.assertThat(strategy.handleActorCreationException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .hasValue(InstanceState.STOPPED);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.RESTARTING);
  }

  @Test
  public void shouldHandlePreStartSignalProcessingExceptionWithMaxRetries() throws InterruptedException {
    final DelayedRestartSupervisionStrategy strategy = new DelayedRestartSupervisionStrategy(1,
        Duration.of(1, ChronoUnit.SECONDS),
        Optional.empty(),
        Optional.empty(),timerExecutorService);

    Assertions.assertThat(strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_START,
        actorInstance,
        actorInstanceContext))
        .isEmpty();

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.STARTING);

    Assertions.assertThat(strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_START,
        actorInstance,
        actorInstanceContext))
        .hasValue(InstanceState.STOPPING);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.STARTING);
  }

  @Test
  public void shouldHandlePreRestartSignalProcessingExceptionWithMaxRetries() throws InterruptedException {
    final DelayedRestartSupervisionStrategy strategy = new DelayedRestartSupervisionStrategy(1,
        Duration.of(1, ChronoUnit.SECONDS),
        Optional.empty(),
        Optional.empty(),timerExecutorService);

    Assertions.assertThat(strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_RESTART,
        actorInstance,
        actorInstanceContext))
        .isEmpty();

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.RESTARTING);

    Assertions.assertThat(strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_RESTART,
        actorInstance,
        actorInstanceContext))
        .hasValue(InstanceState.STOPPING);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.RESTARTING);
  }

  @Test
  public void shouldHandleMessageProcessingExceptionWithoutMaxRetriesWithBackoff() throws InterruptedException {
    final DelayedRestartSupervisionStrategy strategy = new DelayedRestartSupervisionStrategy(0,
        Duration.of(1, ChronoUnit.SECONDS),
        Optional.of(Duration.of(1, ChronoUnit.SECONDS)),
        Optional.empty(),
        timerExecutorService);

    Assertions.assertThat(strategy.handleMessageProcessingException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .isEmpty();

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.RESTARTING);

    Assertions.assertThat(strategy.handleMessageProcessingException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .isEmpty();

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.RESTARTING);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(2))
        .transitionState(InstanceState.RESTARTING);
  }

  @Test
  public void shouldHandleActorCreationExceptionWithoutMaxRetriesWithBackoff() throws InterruptedException {
    final DelayedRestartSupervisionStrategy strategy = new DelayedRestartSupervisionStrategy(0,
        Duration.of(1, ChronoUnit.SECONDS),
        Optional.of(Duration.of(1, ChronoUnit.SECONDS)),
        Optional.empty(),
        timerExecutorService);

    Assertions.assertThat(strategy.handleActorCreationException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .isEmpty();

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.CREATING);

    Assertions.assertThat(strategy.handleActorCreationException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .isEmpty();

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.CREATING);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(2))
        .transitionState(InstanceState.CREATING);
  }

  @Test
  public void shouldHandlePreStartSignalProcessingExceptionWithoutMaxRetriesWithBackoff() throws InterruptedException {
    final DelayedRestartSupervisionStrategy strategy = new DelayedRestartSupervisionStrategy(0,
        Duration.of(1, ChronoUnit.SECONDS),
        Optional.of(Duration.of(1, ChronoUnit.SECONDS)),
        Optional.empty(),
        timerExecutorService);

    Assertions.assertThat(strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_START,
        actorInstance,
        actorInstanceContext))
        .isEmpty();

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.STARTING);

    Assertions.assertThat(strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_START,
        actorInstance,
        actorInstanceContext))
        .isEmpty();

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.STARTING);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(2))
        .transitionState(InstanceState.STARTING);
  }

  @Test
  public void shouldHandlePreRestartSignalProcessingExceptionWithoutMaxRetriesWithBackoff() throws InterruptedException {
    final DelayedRestartSupervisionStrategy strategy = new DelayedRestartSupervisionStrategy(0,
        Duration.of(1, ChronoUnit.SECONDS),
        Optional.of(Duration.of(1, ChronoUnit.SECONDS)),
        Optional.empty(),
        timerExecutorService);

    Assertions.assertThat(strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_RESTART,
        actorInstance,
        actorInstanceContext))
        .isEmpty();

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.RESTARTING);

    Assertions.assertThat(strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_RESTART,
        actorInstance,
        actorInstanceContext))
        .isEmpty();

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.RESTARTING);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(2))
        .transitionState(InstanceState.RESTARTING);
  }

  @Test
  public void shouldHandleMessageProcessingExceptionWithoutMaxRetriesWithBackoffWithFactor() throws InterruptedException {
    final DelayedRestartSupervisionStrategy strategy = new DelayedRestartSupervisionStrategy(0,
        Duration.of(1, ChronoUnit.SECONDS),
        Optional.of(Duration.of(1, ChronoUnit.SECONDS)),
        Optional.of(1),
        timerExecutorService);

    Assertions.assertThat(strategy.handleMessageProcessingException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .isEmpty();

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.RESTARTING);

    Assertions.assertThat(strategy.handleMessageProcessingException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .isEmpty();

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.RESTARTING);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(2))
        .transitionState(InstanceState.RESTARTING);

    Assertions.assertThat(strategy.handleMessageProcessingException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .isEmpty();

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(2))
        .transitionState(InstanceState.RESTARTING);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(2))
        .transitionState(InstanceState.RESTARTING);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(3))
        .transitionState(InstanceState.RESTARTING);
  }

  @Test
  public void shouldHandleActorCreationExceptionWithoutMaxRetriesWithBackoffWithFactor() throws InterruptedException {
    final DelayedRestartSupervisionStrategy strategy = new DelayedRestartSupervisionStrategy(0,
        Duration.of(1, ChronoUnit.SECONDS),
        Optional.of(Duration.of(1, ChronoUnit.SECONDS)),
        Optional.of(1),
        timerExecutorService);

    Assertions.assertThat(strategy.handleActorCreationException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .isEmpty();

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.CREATING);

    Assertions.assertThat(strategy.handleActorCreationException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .isEmpty();

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.CREATING);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(2))
        .transitionState(InstanceState.CREATING);

    Assertions.assertThat(strategy.handleActorCreationException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .isEmpty();

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(2))
        .transitionState(InstanceState.CREATING);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(2))
        .transitionState(InstanceState.CREATING);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(3))
        .transitionState(InstanceState.CREATING);
  }

  @Test
  public void shouldHandlePreStartSignalProcessingExceptionWithoutMaxRetriesWithBackoffWithFactor() throws InterruptedException {
    final DelayedRestartSupervisionStrategy strategy = new DelayedRestartSupervisionStrategy(0,
        Duration.of(1, ChronoUnit.SECONDS),
        Optional.of(Duration.of(1, ChronoUnit.SECONDS)),
        Optional.of(1),
        timerExecutorService);

    Assertions.assertThat(strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_START,
        actorInstance,
        actorInstanceContext))
        .isEmpty();

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.STARTING);

    Assertions.assertThat(strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_START,
        actorInstance,
        actorInstanceContext))
        .isEmpty();

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.STARTING);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(2))
        .transitionState(InstanceState.STARTING);

    Assertions.assertThat(strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_START,
        actorInstance,
        actorInstanceContext))
        .isEmpty();

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(2))
        .transitionState(InstanceState.STARTING);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(2))
        .transitionState(InstanceState.STARTING);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(3))
        .transitionState(InstanceState.STARTING);
  }

  @Test
  public void shouldHandlePreRestartSignalProcessingExceptionWithoutMaxRetriesWithBackoffWithFactor() throws InterruptedException {
    final DelayedRestartSupervisionStrategy strategy = new DelayedRestartSupervisionStrategy(0,
        Duration.of(1, ChronoUnit.SECONDS),
        Optional.of(Duration.of(1, ChronoUnit.SECONDS)),
        Optional.of(1),
        timerExecutorService);

    Assertions.assertThat(strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_RESTART,
        actorInstance,
        actorInstanceContext))
        .isEmpty();

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.RESTARTING);

    Assertions.assertThat(strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_RESTART,
        actorInstance,
        actorInstanceContext))
        .isEmpty();

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.RESTARTING);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(2))
        .transitionState(InstanceState.RESTARTING);

    Assertions.assertThat(strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_RESTART,
        actorInstance,
        actorInstanceContext))
        .isEmpty();

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(2))
        .transitionState(InstanceState.RESTARTING);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(2))
        .transitionState(InstanceState.RESTARTING);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(3))
        .transitionState(InstanceState.RESTARTING);
  }
}
