package io.openactors4j.core.impl.untyped;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.stripToNull;


import io.openactors4j.core.common.Mailbox;
import io.openactors4j.core.common.SupervisionStrategy;
import io.openactors4j.core.impl.system.ActorBuilderContext;
import io.openactors4j.core.untyped.UntypedActor;
import io.openactors4j.core.untyped.UntypedActorBuilder;
import io.openactors4j.core.untyped.UntypedActorRef;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UntypedActorBuilderImpl implements UntypedActorBuilder {
  private final ActorBuilderContext actorBuilderContext;

  private Optional<Class<? extends UntypedActor>> actorClass = empty();
  private Optional<Supplier<? extends UntypedActor>> supplier = empty();
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
  public <T extends UntypedActor> UntypedActorBuilder withSupplier(final Supplier<T> supplier) {
    this.supplier = of(supplier);

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
    this.name = ofNullable(stripToNull(actorName));

    if (this.name.isEmpty() || isBlank(this.name.get())) {
      throw new IllegalArgumentException("Actor name must not be empty or null");
    }

    return this;
  }

  @Override
  public UntypedActorBuilder withNamePrefix(final String actorNamePrefix) {
    if (isBlank(stripToNull(actorNamePrefix))) {
      throw new IllegalArgumentException("Actor name must not be empty or null");
    }

    this.name = of(new StringBuilder()
        .append(actorNamePrefix)
        .append('#')
        .append(UUID.randomUUID().toString().replaceAll("-", "").toLowerCase())
        .toString());
    return this;
  }

  @Override
  public UntypedActorRef create() {
    if (actorClass.isEmpty() && supplier.isEmpty()) {
      throw new IllegalArgumentException("Neither actor class nor instance supplier specified");
    }

    if (actorClass.isPresent() && supplier.isPresent()) {
      throw new IllegalArgumentException("Both actor class and instance supplier specified");
    }

    if (name.isEmpty()) {
      throw new IllegalArgumentException("Actor name must be specified");
    }

    if (actorBuilderContext.haveSiblingWithName(name.get())) {
      throw new IllegalArgumentException("Actor with name '" + name.get() + "' already present");
    }
    ;

    final Supplier<? extends UntypedActor> actorSupplier = this.supplier.orElse(() ->
        (UntypedActor) factory
            .orElse(actorBuilderContext.defaultInstanceFactory())
            .apply(actorClass.get(), arguments.orElse(null)));

    return actorBuilderContext.spawnUntypedActor(this.name.get(), actorSupplier, mailbox, supervisionStrategy);
  }
}
