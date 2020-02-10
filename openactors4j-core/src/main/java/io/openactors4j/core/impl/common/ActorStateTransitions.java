package io.openactors4j.core.impl.common;

import static lombok.AccessLevel.PRIVATE;


import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.tuple.ImmutablePair;

@NoArgsConstructor(access = PRIVATE)
public class ActorStateTransitions {
  private Map<ImmutablePair<InstanceState, InstanceState>, Function<InstanceState, Optional<InstanceState>>> transitionMap = new ConcurrentHashMap<>();

  public static ActorStateTransitions newInstance() {
    return new ActorStateTransitions();
  }

  public ActorStateTransitions addState(final InstanceState startState, final InstanceState destinationState, final Function<InstanceState, Optional<InstanceState>> operator) {
    transitionMap.put(ImmutablePair.of(startState, destinationState), operator);

    return this;
  }

  public Optional<Function<InstanceState, Optional<InstanceState>>> lookup(final InstanceState startState, final InstanceState destinationState) {
    return Optional.ofNullable(transitionMap.get(ImmutablePair.of(startState, destinationState)));
  }
}
