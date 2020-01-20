package io.openactors4j.core.typed;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface BehaviorBuilder<T> {

  BehaviorBuilder withMessage(T message, Function<T, Behavior> handler);

  Behavior<T> onPreRestart(Behavior<T> wrapped, Supplier<Behavior<T>> signalHandler);

  Behavior<T> onPostStop(Behavior<T> wrapped, Consumer<T> signalHandler);

  Behavior<T> build();

  Behavior<T> same();

  Behavior<T> stopped();
}
