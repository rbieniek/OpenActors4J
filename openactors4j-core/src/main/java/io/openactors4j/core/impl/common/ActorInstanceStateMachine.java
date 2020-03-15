package io.openactors4j.core.impl.common;

import static lombok.AccessLevel.PRIVATE;


import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;

@NoArgsConstructor(access = PRIVATE)
@Slf4j
public class ActorInstanceStateMachine {
  private static Map<InstanceState, Boolean> receptionEnabled = new ConcurrentHashMap<>();

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

  private Map<ImmutablePair<InstanceState, InstanceState>, Function<InstanceState, Optional<InstanceState>>> transitionMap = new ConcurrentHashMap<>();

  public static ActorInstanceStateMachine newInstance() {
    return new ActorInstanceStateMachine();
  }

  @Getter
  private InstanceState instanceState = InstanceState.NEW;

  public ActorInstanceStateMachine addState(final InstanceState startState, final InstanceState destinationState, final Function<InstanceState, Optional<InstanceState>> operator) {
    transitionMap.put(ImmutablePair.of(startState, destinationState), operator);

    return this;
  }

  public Optional<Function<InstanceState, Optional<InstanceState>>> lookup(final InstanceState startState, final InstanceState destinationState) {
    return Optional.ofNullable(transitionMap.get(ImmutablePair.of(startState, destinationState)));
  }

  public void transitionState(final InstanceState desiredState, final String actorName) {
    if (instanceState != desiredState) {
      log.info("Transition actor {} from state {} to new state {}",
          instanceState,
          actorName,
          desiredState);
      lookup(instanceState, desiredState)
          .orElseThrow(() -> new IllegalStateException("Cannot transition from state "
              + instanceState
              + " to state "
              + desiredState))
          .apply(desiredState)
          .ifPresent(state -> instanceState = state);
    }
  }

  public ActorInstanceStateMachine presetStateMatrix(final Function<InstanceState, Optional<InstanceState>> function) {
    EnumSet.allOf(InstanceState.class).forEach(key -> {
      EnumSet.allOf(InstanceState.class).forEach(value -> transitionMap.put(ImmutablePair.of(key, value), function));
    });

    return this;
  }

  public void parentalLifecycleEvent(final ParentLifecycleEvent lifecycleEvent) {

  }

  public boolean messageReceptionEnabled() {
    return receptionEnabled.get(instanceState);
  }

  public static Optional<InstanceState> noShift(final InstanceState desiredInstanceState) {
    return Optional.empty();
  }

  public static Optional<InstanceState> shift(final InstanceState desiredInstanceState) {
    return Optional.of(desiredInstanceState);
  }
}
