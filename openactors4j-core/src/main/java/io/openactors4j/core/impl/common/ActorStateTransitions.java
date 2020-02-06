package io.openactors4j.core.impl.common;

import static lombok.AccessLevel.PRIVATE;


import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.tuple.ImmutablePair;

@NoArgsConstructor(access = PRIVATE)
public class ActorStateTransitions {
  private Map<ImmutablePair<InstanceState, InstanceState>, UnaryOperator<InstanceState>> transitionMap = new ConcurrentHashMap<>();

  public static ActorStateTransitions newInstance() {
    return new ActorStateTransitions();
  }

  public ActorStateTransitions addState(final InstanceState startState, final InstanceState destinationState, final UnaryOperator<InstanceState> operator) {
    transitionMap.put(ImmutablePair.of(startState, destinationState), operator);

    return this;
  }

  public Optional<UnaryOperator<InstanceState>> lookup(final InstanceState startState, final InstanceState destinationState) {
    return Optional.ofNullable(transitionMap.get(ImmutablePair.of(startState, destinationState)));
  }
}
