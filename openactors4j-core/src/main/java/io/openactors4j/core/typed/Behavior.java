package io.openactors4j.core.typed;

import io.openactors4j.core.common.Actor;
import io.openactors4j.core.common.ActorContext;
import io.openactors4j.core.common.Signal;

public interface Behavior<T> extends Actor {
  Behavior<T> receive(T message);

}