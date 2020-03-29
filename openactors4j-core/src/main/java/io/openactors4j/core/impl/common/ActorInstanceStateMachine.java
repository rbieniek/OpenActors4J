package io.openactors4j.core.impl.common;

import static lombok.AccessLevel.PRIVATE;


import io.openactors4j.core.common.Signal;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;

@NoArgsConstructor(access = PRIVATE)
@Slf4j
public class ActorInstanceStateMachine {
  private static final Map<InstanceState, Boolean> receptionEnabled = new ConcurrentHashMap<>();

  private final Map<ImmutablePair<InstanceState, InstanceState>, StateTransitionAction> transitionMap = new ConcurrentHashMap<>();

  public static ActorInstanceStateMachine newInstance() {
    return new ActorInstanceStateMachine();
  }

  private final BlockingDeque<QueuedState> stateQueue = new LinkedBlockingDeque<>();
  private final Semaphore messageWaiting = new Semaphore(1);
  private final AtomicBoolean allowRunning = new AtomicBoolean(false);

  private Thread processingThread;

  @Getter
  private InstanceState instanceState = InstanceState.NEW;

  @Getter
  @Setter
  private String actorName;

  @FunctionalInterface
  public interface StateTransitionAction {
    void accept(InstanceState sourceState, InstanceState targetState,
                Optional<TransitionContext> transitionContext);
  }

  @Data
  @Builder
  public static class TransitionContext {
    private Throwable throwable;
    private Signal signal;
  }

  static {
    receptionEnabled.put(InstanceState.NEW, false);
    receptionEnabled.put(InstanceState.CREATING, true);
    receptionEnabled.put(InstanceState.CREATE_DELAYED, true);
    receptionEnabled.put(InstanceState.CREATE_FAILED, true);
    receptionEnabled.put(InstanceState.STARTING, true);
    receptionEnabled.put(InstanceState.START_FAILED, true);
    receptionEnabled.put(InstanceState.RUNNING, true);
    receptionEnabled.put(InstanceState.PROCESSING_FAILED, true);
    receptionEnabled.put(InstanceState.RESTARTING, true);
    receptionEnabled.put(InstanceState.RESTARTING_FAILED, true);
    receptionEnabled.put(InstanceState.CREATING, true);
    receptionEnabled.put(InstanceState.CREATING, true);
    receptionEnabled.put(InstanceState.STOPPING, false);
    receptionEnabled.put(InstanceState.STOPPED, false);
  }

  public ActorInstanceStateMachine addState(final InstanceState startState,
                                            final InstanceState destinationState,
                                            final StateTransitionAction operator) {
    transitionMap.put(ImmutablePair.of(startState, destinationState), operator);

    return this;
  }

  public Optional<StateTransitionAction> lookup(final InstanceState startState,
                                                final InstanceState destinationState) {
    return Optional.ofNullable(transitionMap.get(ImmutablePair.of(startState, destinationState)));
  }

  public void transitionState(final InstanceState desiredState) {
    transitionState(desiredState, Optional.empty());
  }

  public void transitionState(final InstanceState desiredState, final Throwable throwable) {
    transitionState(desiredState, Optional.of(TransitionContext.builder()
        .throwable(throwable)
        .build()));
  }

  public void transitionState(final InstanceState desiredState, final Signal signal, final Throwable throwable) {
    transitionState(desiredState, Optional.of(TransitionContext.builder()
        .throwable(throwable)
        .signal(signal)
        .build()));
  }

  public void transitionState(final InstanceState desiredState,
                              final Optional<TransitionContext> transitionContext) {
    stateQueue.add(QueuedState.builder()
        .targetState(desiredState)
        .context(transitionContext)
        .build());
    messageWaiting.release();
  }


  public ActorInstanceStateMachine presetStateMatrix(final StateTransitionAction function) {
    EnumSet.allOf(InstanceState.class).forEach(key -> {
      EnumSet.allOf(InstanceState.class)
          .forEach(value -> transitionMap.put(ImmutablePair.of(key, value), function));
    });

    return this;
  }

  public void start() {
    allowRunning.set(true);

    restarProcessingThread();
  }

  private void restarProcessingThread() {
    if (allowRunning.get()) {
      processingThread = new Thread(new ProcessLoop());

      processingThread.setUncaughtExceptionHandler(new RestartProcessingThreadHandler());
      processingThread.start();
    }
  }

  public void stop() {
    allowRunning.set(false);
    messageWaiting.release();
  }

  public void parentalLifecycleEvent(final ParentLifecycleEvent lifecycleEvent) {

  }

  public boolean messageReceptionEnabled() {
    return receptionEnabled.get(instanceState);
  }

  private void moveState(final QueuedState queuedState) {
    log.info("Transition actor {} from state {} to new state {}",
        instanceState,
        actorName,
        queuedState.getTargetState());
    lookup(instanceState, queuedState.getTargetState())
        .orElseThrow(() -> new IllegalStateException("Cannot transition from state "
            + instanceState
            + " to state "
            + queuedState.getTargetState()))
        .accept(instanceState, queuedState.getTargetState(), queuedState.getContext());
  }

  private class ProcessLoop implements Runnable {
    @Override
    public void run() {
      boolean shoudRun = true;

      while (shoudRun) {
        try {
          messageWaiting.acquire();

          shoudRun = allowRunning.get();

          if (shoudRun) {
            QueuedState state;

            while ((state = stateQueue.pollFirst(100, TimeUnit.MILLISECONDS)) != null) {
              moveState(state);
            }
          }
        } catch (InterruptedException e) {
          log.info("Message processing thread interrupted", e);

          shoudRun = allowRunning.get();
        }
      }
    }
  }

  private class RestartProcessingThreadHandler implements Thread.UncaughtExceptionHandler {
    @Override
    public void uncaughtException(Thread t, Throwable e) {
      log.info("Processing thread {} died with uncaught exception", e);

      restarProcessingThread();
    }
  }

  @Data
  @Builder
  private static class QueuedState {
    private InstanceState targetState;
    @Builder.Default
    private Optional<TransitionContext> context = Optional.empty();
  }
}
