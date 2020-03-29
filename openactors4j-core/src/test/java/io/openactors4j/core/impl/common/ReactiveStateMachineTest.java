package io.openactors4j.core.impl.common;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.Builder;
import lombok.Data;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ReactiveStateMachineTest {
  private enum States {
    NEW,
    START,
    RUNNING,
    STOPPED,
    FINISHED;
  }

  private interface Methods {
    void stateNewToStart(final States source, final States target,
                         final ReactiveStateMachine.ReactiveStateUpdater<States, TestData> updater,
                         final Optional<TestData> data);

    void stateStartToRunning(final States source, final States target,
                             final ReactiveStateMachine.ReactiveStateUpdater<States, TestData> updater,
                             final Optional<TestData> data);

    void stateRunningToStopped(final States source, final States target,
                               final ReactiveStateMachine.ReactiveStateUpdater<States, TestData> updater,
                               final Optional<TestData> data);

    void stateStoppedToFinished(final States source, final States target,
                                final ReactiveStateMachine.ReactiveStateUpdater<States, TestData> updater,
                                final Optional<TestData> data);

    void dontMove(final States source, final States target,
                  final Optional<TestData> data);
  }

  private ReactiveStateMachine rsm;
  private Methods methods;

  @BeforeEach
  public void setupStateMachine() {
    methods = Mockito.mock(Methods.class);
    rsm = new ReactiveStateMachine<States, TestData>(Executors.newCachedThreadPool(), "rsm")
        .addState(States.NEW, States.START, methods::stateNewToStart)
        .addState(States.START, States.RUNNING, methods::stateStartToRunning)
        .addState(States.RUNNING, States.STOPPED, methods::stateRunningToStopped)
        .addState(States.STOPPED, States.FINISHED, methods::stateStoppedToFinished)
        .start(States.NEW);
  }

  @Test
  public void shouldExecuteSingleStateTransition() {
    Assertions.assertThat(rsm.getCurrentState()).isEqualTo(States.NEW);

    rsm.postStateTransition(States.START);

    Awaitility.await()
        .atMost(5, TimeUnit.SECONDS)
        .until(() -> rsm.getCurrentState() == States.START);

    Mockito.verify(methods, Mockito.times(1))
        .stateNewToStart(Mockito.eq(States.NEW),
            Mockito.eq(States.START),
            Mockito.any(ReactiveStateMachine.ReactiveStateUpdater.class),
            Mockito.eq(Optional.empty()));

  }

  @Test
  public void shouldExecuteSingleStateTransitionWithContext() {
    Assertions.assertThat(rsm.getCurrentState()).isEqualTo(States.NEW);

    rsm.postStateTransition(States.START, TestData.builder()
        .number(1)
        .build());

    Awaitility.await()
        .atMost(5, TimeUnit.SECONDS)
        .until(() -> rsm.getCurrentState() == States.START);

    Mockito.verify(methods, Mockito.times(1))
        .stateNewToStart(Mockito.eq(States.NEW),
            Mockito.eq(States.START),
            Mockito.any(ReactiveStateMachine.ReactiveStateUpdater.class),
            Mockito.eq(Optional.of(TestData.builder()
                .number(1)
                .build())));
  }

  @Test
  public void shouldExecuteStateCycleTransitionWithContext() {
    Assertions.assertThat(rsm.getCurrentState()).isEqualTo(States.NEW);

    rsm.postStateTransition(States.START, TestData.builder()
        .number(1)
        .build());

    Awaitility.await()
        .atMost(5, TimeUnit.SECONDS)
        .until(() -> rsm.getCurrentState() == States.START);

    rsm.postStateTransition(States.RUNNING, TestData.builder()
        .number(2)
        .build());

    Awaitility.await()
        .atMost(5, TimeUnit.SECONDS)
        .until(() -> rsm.getCurrentState() == States.RUNNING);

    rsm.postStateTransition(States.STOPPED, TestData.builder()
        .number(3)
        .build());

    Awaitility.await()
        .atMost(5, TimeUnit.SECONDS)
        .until(() -> rsm.getCurrentState() == States.STOPPED);

    rsm.postStateTransition(States.FINISHED, TestData.builder()
        .number(4)
        .build());

    Awaitility.await()
        .atMost(5, TimeUnit.SECONDS)
        .until(() -> rsm.getCurrentState() == States.FINISHED);

    Mockito.verify(methods, Mockito.times(1))
        .stateNewToStart(Mockito.eq(States.NEW),
            Mockito.eq(States.START),
            Mockito.any(ReactiveStateMachine.ReactiveStateUpdater.class),
            Mockito.eq(Optional.of(TestData.builder()
                .number(1)
                .build())));
    Mockito.verify(methods, Mockito.times(1))
        .stateStartToRunning(Mockito.eq(States.START),
            Mockito.eq(States.RUNNING),
            Mockito.any(ReactiveStateMachine.ReactiveStateUpdater.class),
            Mockito.eq(Optional.of(TestData.builder()
                .number(2)
                .build())));
    Mockito.verify(methods, Mockito.times(1))
        .stateRunningToStopped(Mockito.eq(States.RUNNING),
            Mockito.eq(States.STOPPED),
            Mockito.any(ReactiveStateMachine.ReactiveStateUpdater.class),
            Mockito.eq(Optional.of(TestData.builder()
                .number(3)
                .build())));
    Mockito.verify(methods, Mockito.times(1))
        .stateStoppedToFinished(Mockito.eq(States.STOPPED),
            Mockito.eq(States.FINISHED),
            Mockito.any(ReactiveStateMachine.ReactiveStateUpdater.class),
            Mockito.eq(Optional.of(TestData.builder()
                .number(4)
                .build())));
  }

  @Test
  public void shouldExecuteSingleStateTransitionWithUpdateTransition() {

    Mockito.doAnswer(invocation -> {
      final ReactiveStateMachine.ReactiveStateUpdater<States, TestData> updater =
          invocation.getArgument(2, ReactiveStateMachine.ReactiveStateUpdater.class);
      final Optional<TestData> context = invocation.getArgument(3, Optional.class);

      updater.postStateTransition(States.RUNNING, context.orElse(null));
      return null;
    })
        .when(methods)
        .stateNewToStart(Mockito.eq(States.NEW),
            Mockito.eq(States.START),
            Mockito.any(ReactiveStateMachine.ReactiveStateUpdater.class),
            Mockito.any(Optional.class));

    Assertions.assertThat(rsm.getCurrentState()).isEqualTo(States.NEW);

    rsm.postStateTransition(States.START);

    Awaitility.await()
        .atMost(5, TimeUnit.SECONDS)
        .until(() -> rsm.getCurrentState() == States.RUNNING);

    Mockito.verify(methods, Mockito.times(1))
        .stateNewToStart(Mockito.eq(States.NEW),
            Mockito.eq(States.START),
            Mockito.any(ReactiveStateMachine.ReactiveStateUpdater.class),
            Mockito.eq(Optional.empty()));
    Mockito.verify(methods, Mockito.times(1))
        .stateStartToRunning(Mockito.eq(States.START),
            Mockito.eq(States.RUNNING),
            Mockito.any(ReactiveStateMachine.ReactiveStateUpdater.class),
            Mockito.eq(Optional.empty()));
  }

  @Test
  public void shouldExecuteSingleStateTransitionWithUpdateTransitionAndContext() {

    Mockito.doAnswer(invocation -> {
      final ReactiveStateMachine.ReactiveStateUpdater<States, TestData> updater =
          invocation.getArgument(2, ReactiveStateMachine.ReactiveStateUpdater.class);
      final Optional<TestData> context = invocation.getArgument(3, Optional.class);

      updater.postStateTransition(States.RUNNING, context.orElse(null));
      return null;
    })
        .when(methods)
        .stateNewToStart(Mockito.eq(States.NEW),
            Mockito.eq(States.START),
            Mockito.any(ReactiveStateMachine.ReactiveStateUpdater.class),
            Mockito.any(Optional.class));

    Assertions.assertThat(rsm.getCurrentState()).isEqualTo(States.NEW);

    rsm.postStateTransition(States.START, TestData.builder()
        .text("foo")
        .build());

    Awaitility.await()
        .atMost(5, TimeUnit.SECONDS)
        .until(() -> rsm.getCurrentState() == States.RUNNING);

    Mockito.verify(methods, Mockito.times(1))
        .stateNewToStart(Mockito.eq(States.NEW),
            Mockito.eq(States.START),
            Mockito.any(ReactiveStateMachine.ReactiveStateUpdater.class),
            Mockito.eq(Optional.of(TestData.builder()
                .text("foo")
                .build())));
    Mockito.verify(methods, Mockito.times(1))
        .stateStartToRunning(Mockito.eq(States.START),
            Mockito.eq(States.RUNNING),
            Mockito.any(ReactiveStateMachine.ReactiveStateUpdater.class),
            Mockito.eq(Optional.of(TestData.builder()
                .text("foo")
                .build())));
  }

  @Test
  public void shouldNotMoveToUnknownState() throws InterruptedException {
    rsm.setDefaultAction((ReactiveStateMachine.ReactiveStateDefaultTransitionAction<States, TestData>) methods::dontMove);

    rsm.postStateTransition(States.RUNNING, TestData.builder()
        .text("foo")
        .build());

    Thread.sleep(100);

    Mockito.verify(methods,
        Mockito.times(1))
        .dontMove(Mockito.eq(States.NEW),
            Mockito.eq(States.RUNNING),
            Mockito.eq(Optional.of(TestData.builder()
                .text("foo")
                .build())));
  }

  @Data
  @Builder
  public static class TestData implements ReactiveStateMachine.ReactiveTransitionContext {
    private int number;
    private String text;
  }
}
