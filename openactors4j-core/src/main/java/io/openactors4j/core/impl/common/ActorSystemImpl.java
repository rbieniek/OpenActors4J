package io.openactors4j.core.impl.common;

import io.openactors4j.core.common.ActorSystem;
import io.openactors4j.core.common.SystemAddress;
import io.openactors4j.core.typed.Behavior;
import io.openactors4j.core.typed.Behaviors;
import io.openactors4j.core.typed.TypedActorRef;
import io.openactors4j.core.untyped.UntypedActorBuilder;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ActorSystemImpl implements ActorSystem {

  private final String name;
  private final BiFunction factory;
  private final ExecutorService executorService;
  private final Consumer<Throwable> unrecoverableErrorHandler;

  @Override
  public String name() {
    return null;
  }

  @Override
  public SystemAddress[] adress() {
    return new SystemAddress[0];
  }

  @Override
  public <T> Behaviors behaviors() {
    return null;
  }

  @Override
  public <T, C extends Behavior<T>> TypedActorRef<T> spawn(Behavior<T> behavior, String name) {
    return null;
  }

  @Override
  public UntypedActorBuilder newUntypedActor() {
    return null;
  }

  void start() {

  }

  @Override
  public void shutown() {

  }
}
