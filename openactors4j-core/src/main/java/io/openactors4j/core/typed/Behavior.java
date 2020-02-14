package io.openactors4j.core.typed;

import io.openactors4j.core.common.Actor;

public interface Behavior<T> extends Actor {
  Behavior<T> receive(T message);
}