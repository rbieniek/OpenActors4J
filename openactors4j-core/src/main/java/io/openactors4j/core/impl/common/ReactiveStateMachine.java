package io.openactors4j.core.impl.common;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * Build a state machine build upon a reactive streams pattern to allow transparent decoupling
 * between the state change initiator and the actual executed state transitions
 *
 * @param <T> the event type the state machine acts upon
 */
@Slf4j
public class ReactiveStateMachine<T, V extends ReactiveStateMachine.ReactiveTransitionContext> {

  private final Map<ImmutablePair<T, T>,
      ReactiveStateTransitionAction<T, V>> stateTransitions = new ConcurrentHashMap<>();

  private final SubmissionPublisher<QueuedStateTransition<T, V>> publisher;

  @Getter
  private final String name;

  /**
   * Default action
   */
  @NonNull
  private ReactiveStateDefaultTransitionAction<T, V> defaultAction = this::nothing;

  @Getter
  private T currentState;

  /**
   * Tag interface for contextual information passed along a state transition
   */
  public interface ReactiveTransitionContext {
  }

  /**
   * Callback interface to a allow a state transition method to submit a state transition
   *
   * @param <T>
   * @param <V>
   */
  public interface ReactiveStateUpdater<T, V extends ReactiveTransitionContext> {
    void postStateTransition(final T newState, final V context);
  }

  /**
   * Define actions to be taken when a state shift is executed
   *
   * @param <T>
   */
  @FunctionalInterface
  public interface ReactiveStateTransitionAction<T, V extends ReactiveTransitionContext> {
    void accept(T sourceState, T targetState,
                ReactiveStateUpdater<T, V> stateUpdater,
                Optional<V> transitionContext);
  }

  /**
   * Define actions to be taken when a state shift is executed
   *
   * @param <T>
   */
  @FunctionalInterface
  public interface ReactiveStateDefaultTransitionAction<T, V extends ReactiveTransitionContext> {
    void accept(T sourceState, T targetState,
                Optional<V> transitionContext);
  }

  public ReactiveStateMachine(final ExecutorService executorService, final String name) {
    publisher = new SubmissionPublisher<>(executorService, Flow.defaultBufferSize());
    this.name = name;
  }

  /**
   * Start the given machine with an initial state
   *
   * @param initialState the initial state of the machine
   * @return the machine instance ready for method chaining
   */
  public ReactiveStateMachine<T, V> start(final T initialState) {
    currentState = initialState;
    publisher.subscribe(new ReactiveStateSubscriber());

    return this;
  }

  /**
   * Stop the machine and disable any further state processing
   */
  public void stop() {
    publisher.close();
  }

  public ReactiveStateMachine<T, V> setDefaultAction(final @NonNull ReactiveStateDefaultTransitionAction<T, V> defaultAction) {
    this.defaultAction = defaultAction;

    return this;
  }

  /**
   * Add a state transition to the machine
   *
   * @param sourceState the state from which the transition shall occur
   * @param targetState the state to which the transition shall ooccur
   * @param action      the action to be executed on a state change
   * @return the machine instance ready for method chaining
   */
  public ReactiveStateMachine<T, V> addState(final @NonNull T sourceState,
                                             final @NonNull T targetState,
                                             final @NonNull ReactiveStateTransitionAction<T, V> action) {
    final ImmutablePair<T, T> key = ImmutablePair.of(sourceState, targetState);

    if (stateTransitions.containsKey(key)) {
      throw new IllegalArgumentException(String.format("State transition from %s to %s already defined",
          sourceState, targetState));
    }

    stateTransitions.put(key, action);

    return this;
  }


  /**
   * Post a state transition to this machine.
   * <p>
   * The state transition will not happen on the caller thread.
   *
   * @param newState the state to move into
   */
  public void postStateTransition(final T newState) {
    postStateTransition(newState, null);
  }

  /**
   * Post a state transition to this machine.
   * <p>
   * The state transition will not happen on the caller thread.
   *
   * @param newState the state to move into
   * @param context  a contextual object to be passed to the executed transition method
   */
  public void postStateTransition(final T newState, final V context) {
    publisher.submit(QueuedStateTransition.<T, V>builder()
        .state(newState)
        .context(Optional.ofNullable(context))
        .build());
  }

  private void nothing(final T sourceState,
                       final T targetState,
                       final Optional<V> context) {
    log.info("do nothing default state action for transition from stsate {} to state {}",
        sourceState, targetState);
  }

  private ReactiveStateTransitionAction<T, V> wrapDefaultAction() {
    return new ReactiveStateTransitionAction<T, V>() {
      @Override
      public void accept(T sourceState, T targetState, ReactiveStateUpdater<T, V> stateUpdater, Optional<V> transitionContext) {
        defaultAction.accept(sourceState, targetState, transitionContext);
      }
    };
  }

  private void moveState(QueuedStateTransition<T, V> stateTransition) {
    final T nextState = stateTransition.getState();

    log.info("Move state machine {} from state {} to state {}", name, currentState, nextState);

    Optional.ofNullable(stateTransitions.get(ImmutablePair.of(currentState,
        nextState)))
        .orElse(wrapDefaultAction())
        .accept(currentState, nextState, this::callbackPostStateTransition,
            stateTransition.getContext());

    currentState = nextState;
  }

  private void callbackPostStateTransition(final T newState, final V context) {
    if (!publisher.isClosed()) {
      publisher.submit(QueuedStateTransition.<T, V>builder()
          .state(newState)
          .context(Optional.ofNullable(context))
          .build());
    }
  }

  private class ReactiveStateSubscriber implements Flow.Subscriber<QueuedStateTransition<T, V>> {
    private Flow.Subscription subscription;

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
      this.subscription = subscription;

      subscription.request(1);
    }

    @Override
    public void onNext(QueuedStateTransition<T, V> item) {
      final T currentState = getCurrentState();

      try {
        moveState(item);
      } catch (Exception e) {
        log.error("Exception for machine {} in state transition from state {} to state {}",
            name,
            currentState,
            item.getState());
      }

      subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onComplete() {
      log.info("Procssing complete for machine {}", name);
      log.info("Procssing complete for machine {}", name);
    }
  }

  @Builder
  @Data
  private static class QueuedStateTransition<T, V extends ReactiveTransitionContext> {
    private T state;
    private Optional<V> context;
  }
}
