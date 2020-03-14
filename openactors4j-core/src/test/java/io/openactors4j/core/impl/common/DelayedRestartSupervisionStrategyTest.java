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
        .isEqualTo(InstanceState.RESTARTING_DELAYED);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.RUNNING);

    Assertions.assertThat(strategy.handleMessageProcessingException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .isEqualTo(InstanceState.RESTARTING_DELAYED);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(2))
        .transitionState(InstanceState.RUNNING);
  }


  @Test
  public void shouldHandleSignalProcessingExceptionWithoutMaxRetries() throws InterruptedException {
    final DelayedRestartSupervisionStrategy strategy = new DelayedRestartSupervisionStrategy(0,
        Duration.of(1, ChronoUnit.SECONDS),
        Optional.empty(),
        Optional.empty(),timerExecutorService);

    Assertions.assertThat(strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_START,
        actorInstance,
        actorInstanceContext))
        .isEqualTo(InstanceState.RESTARTING_DELAYED);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.RUNNING);


    Assertions.assertThat(strategy.handleMessageProcessingException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .isEqualTo(InstanceState.RESTARTING_DELAYED);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(2))
        .transitionState(InstanceState.RUNNING);
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
        .isEqualTo(InstanceState.RESTARTING_DELAYED);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.RUNNING);

    Assertions.assertThat(strategy.handleMessageProcessingException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .isEqualTo(InstanceState.STOPPED);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.RUNNING);
  }


  @Test
  public void shouldHandleSignalProcessingExceptionWithMaxRetries() throws InterruptedException {
    final DelayedRestartSupervisionStrategy strategy = new DelayedRestartSupervisionStrategy(1,
        Duration.of(1, ChronoUnit.SECONDS),
        Optional.empty(),
        Optional.empty(),timerExecutorService);

    Assertions.assertThat(strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_START,
        actorInstance,
        actorInstanceContext))
        .isEqualTo(InstanceState.RESTARTING_DELAYED);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.RUNNING);

    Assertions.assertThat(strategy.handleMessageProcessingException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .isEqualTo(InstanceState.STOPPED);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.RUNNING);
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
        .isEqualTo(InstanceState.RESTARTING_DELAYED);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.RUNNING);

    Assertions.assertThat(strategy.handleMessageProcessingException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .isEqualTo(InstanceState.RESTARTING_DELAYED);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.RUNNING);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(2))
        .transitionState(InstanceState.RUNNING);
  }

  @Test
  public void shouldHandleSignalProcessingExceptionWithoutMaxRetriesWithBackoff() throws InterruptedException {
    final DelayedRestartSupervisionStrategy strategy = new DelayedRestartSupervisionStrategy(0,
        Duration.of(1, ChronoUnit.SECONDS),
        Optional.of(Duration.of(1, ChronoUnit.SECONDS)),
        Optional.empty(),
        timerExecutorService);

    Assertions.assertThat(strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_START,
        actorInstance,
        actorInstanceContext))
        .isEqualTo(InstanceState.RESTARTING_DELAYED);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.RUNNING);

    Assertions.assertThat(strategy.handleMessageProcessingException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .isEqualTo(InstanceState.RESTARTING_DELAYED);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.RUNNING);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(2))
        .transitionState(InstanceState.RUNNING);
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
        .isEqualTo(InstanceState.RESTARTING_DELAYED);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.RUNNING);

    Assertions.assertThat(strategy.handleMessageProcessingException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .isEqualTo(InstanceState.RESTARTING_DELAYED);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.RUNNING);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(2))
        .transitionState(InstanceState.RUNNING);

    Assertions.assertThat(strategy.handleMessageProcessingException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .isEqualTo(InstanceState.RESTARTING_DELAYED);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(2))
        .transitionState(InstanceState.RUNNING);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(2))
        .transitionState(InstanceState.RUNNING);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(3))
        .transitionState(InstanceState.RUNNING);
  }

  @Test
  public void shouldHandleSignalProcessingExceptionWithoutMaxRetriesWithBackoffWithFactor() throws InterruptedException {
    final DelayedRestartSupervisionStrategy strategy = new DelayedRestartSupervisionStrategy(0,
        Duration.of(1, ChronoUnit.SECONDS),
        Optional.of(Duration.of(1, ChronoUnit.SECONDS)),
        Optional.of(1),
        timerExecutorService);

    Assertions.assertThat(strategy.handleSignalProcessingException(new Exception(),
        Signal.PRE_START,
        actorInstance,
        actorInstanceContext))
        .isEqualTo(InstanceState.RESTARTING_DELAYED);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.RUNNING);

    Assertions.assertThat(strategy.handleMessageProcessingException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .isEqualTo(InstanceState.RESTARTING_DELAYED);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(1))
        .transitionState(InstanceState.RUNNING);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(2))
        .transitionState(InstanceState.RUNNING);
    Assertions.assertThat(strategy.handleMessageProcessingException(new Exception(),
        actorInstance,
        actorInstanceContext))
        .isEqualTo(InstanceState.RESTARTING_DELAYED);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(2))
        .transitionState(InstanceState.RUNNING);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(2))
        .transitionState(InstanceState.RUNNING);

    Thread.sleep(1100L);

    Mockito.verify(actorInstance,
        Mockito.times(3))
        .transitionState(InstanceState.RUNNING);
  }
}
