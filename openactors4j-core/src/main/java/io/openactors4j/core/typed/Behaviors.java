package io.openactors4j.core.typed;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface Behaviors {
  <T> Behavior<T> onPreStart(Behavior<T> wrapped, Supplier<Behavior<T>> signalHandler);

  <T> Behavior<T> onPreRestart(Behavior<T> wrapped, Supplier<Behavior<T>> signalHandler);

  <T> Behavior<T> onPostStop(Behavior<T> wrapped, Consumer<T> signalHandler);

  <T> Behavior<T> withMessage(Behavior<T> wrapped, Function<T, Behavior<T>> messageHandler);
}
