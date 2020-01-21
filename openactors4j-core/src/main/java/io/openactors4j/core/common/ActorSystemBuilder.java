package io.openactors4j.core.common;

import io.openactors4j.core.untyped.UntypedActor;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public interface ActorSystemBuilder {

  ActorSystemBuilder withName(String name);

  <T extends UntypedActor> ActorSystemBuilder withUntypedActorFactory(BiFunction<Class<T>, Object[], T> factory);

  ActorSystemBuilder withUserThreadPoolConfiguration(ThreadPoolConfiguration parameters);

  ActorSystemBuilder withSystemThreadPoolConfiguration(ThreadPoolConfiguration parameters);

  ActorSystemBuilder withUnrecoverableErrorHandler(Consumer<Throwable> handler);

  ActorSystem build();
}
