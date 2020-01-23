package io.openactors4j.core.impl.untyped;

import static java.util.Optional.empty;
import static java.util.Optional.of;


import io.openactors4j.core.common.Mailbox;
import io.openactors4j.core.common.SupervisionStrategy;
import io.openactors4j.core.impl.common.ActorSystemImpl;
import io.openactors4j.core.untyped.UntypedActor;
import io.openactors4j.core.untyped.UntypedActorBuilder;
import io.openactors4j.core.untyped.UntypedActorRef;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UntypedActorBuilderImpl implements UntypedActorBuilder {
  private final ActorSystemImpl actorSystem;

  private Optional<Class<? extends UntypedActor>> actorClass = empty();
  private Optional<UntypedActor> actorInstance = empty();
  private Optional<BiFunction> factory = empty();
  private Optional<Object[]> arguments = empty();
  private Optional<SupervisionStrategy> supervisionStrategy = empty();
  private Optional<Mailbox> mailbox = empty();
  private Optional<String> name = empty();

  @Override
  public <T extends UntypedActor> UntypedActorBuilder withActorClass(final Class<T> actorClass) {
    this.actorClass = of(actorClass);

    return this;
  }

  @Override
  public <T extends UntypedActor> UntypedActorBuilder withActorInstance(final T actorInstance) {
    return null;
  }

  @Override
  public <T extends UntypedActor> UntypedActorBuilder withFactory(final BiFunction<Class<T>, Object[], T> factory) {
    this.factory = of(factory);

    return this;
  }

  @Override
  public UntypedActorBuilder withArguments(final Object... arguments) {
    this.arguments = of(arguments);

    return this;
  }

  @Override
  public UntypedActorBuilder withSupervisionStrategy(final SupervisionStrategy strategy) {
    this.supervisionStrategy = of(strategy);

    return this;
  }

  @Override
  public UntypedActorBuilder withMailbox(final Mailbox<Object> mailbox) {
    this.mailbox = of(mailbox);

    return this;
  }

  @Override
  public UntypedActorBuilder withAbsoluteName(final String actorName) {
    this.name = of(actorName);

    return this;
  }

  @Override
  public UntypedActorBuilder withNamePrefix(final String actorNamePrefix) {
    this.name = of((new StringBuilder()
        .append(actorNamePrefix)
        .append('-')
        .append(UUID.randomUUID().toString().replaceAll("-", ""))
        .toString()));
    return this;
  }

  @Override
  public UntypedActorRef create() {
    if(actorClass.isEmpty() && actorInstance.isEmpty()) {
      throw new IllegalArgumentException("Neither actor class nor instance specified");
    }

    if(actorClass.isPresent() && actorInstance.isPresent()) {
      throw new IllegalArgumentException("Both actor class and instance specified");
    }

    return null;
  }
}
