package io.openactors4j.core.impl.untyped;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.regex.Pattern.compile;
import static org.assertj.core.api.Assertions.assertThat;


import io.openactors4j.core.common.ActorContext;
import io.openactors4j.core.common.ActorRef;
import io.openactors4j.core.common.Mailbox;
import io.openactors4j.core.common.Signal;
import io.openactors4j.core.common.StartupMode;
import io.openactors4j.core.common.SupervisionStrategy;
import io.openactors4j.core.common.UnboundedMailbox;
import io.openactors4j.core.impl.common.ActorInstanceContext;
import io.openactors4j.core.impl.common.ActorInstanceStateTransition;
import io.openactors4j.core.impl.system.ActorBuilderContext;
import io.openactors4j.core.impl.system.SupervisionStrategyInternal;
import io.openactors4j.core.untyped.UntypedActor;
import io.openactors4j.core.untyped.UntypedActorBuilder;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class UntypedActorBuildImplTest {

  @Test
  public void shouldSpawnActorWithSupplierWithDefaultMailboxWithDefaultSupervision() {
    final UntypedActorBuilder actorBuilder = new UntypedActorBuilderImpl(
        TestActorBuilderContext.builder()
            .expectedActorNamePattern(() -> compile("^test-actor$"))
            .shoudHaveEmptySupervisionStrategy(true)
            .shoudHaveEmptyMailbox(true)
            .build()
    );

    assertThat(actorBuilder
        .withAbsoluteName("test-actor")
        .withSupplier(() -> TestUntypedActor.builder()
            .tag("test-actor")
            .build())
        .create())
        .isNotNull()
        .isInstanceOf(ActorRef.class)
        .extracting(ar -> ar.name())
        .isEqualTo("test-actor");
  }

  @Test
  public void shouldSpawnActorWithSupplierWithDefaultMailboxWithDefaultSupervisionWithDelayedStartup() {
    final UntypedActorBuilder actorBuilder = new UntypedActorBuilderImpl(
        TestActorBuilderContext.builder()
            .expectedActorNamePattern(() -> compile("^test-actor$"))
            .shoudHaveEmptySupervisionStrategy(true)
            .shoudHaveEmptyMailbox(true)
            .startupMode(of(StartupMode.DELAYED))
            .build()
    );

    assertThat(actorBuilder
        .withAbsoluteName("test-actor")
        .withSupplier(() -> TestUntypedActor.builder()
            .tag("test-actor")
            .build())
        .withStartupMode(StartupMode.DELAYED)
        .create())
        .isNotNull()
        .isInstanceOf(ActorRef.class)
        .extracting(ar -> ar.name())
        .isEqualTo("test-actor");
  }

  @Test
  public void shouldSpawnActorWithSupplierWithDefaultMailboxWithDefaultSupervisionWithImmediateStartup() {
    final UntypedActorBuilder actorBuilder = new UntypedActorBuilderImpl(
        TestActorBuilderContext.builder()
            .expectedActorNamePattern(() -> compile("^test-actor$"))
            .shoudHaveEmptySupervisionStrategy(true)
            .shoudHaveEmptyMailbox(true)
            .startupMode(of(StartupMode.IMMEDIATE))
            .build()
    );

    assertThat(actorBuilder
        .withAbsoluteName("test-actor")
        .withSupplier(() -> TestUntypedActor.builder()
            .tag("test-actor")
            .build())
        .withStartupMode(StartupMode.IMMEDIATE)
        .create())
        .isNotNull()
        .isInstanceOf(ActorRef.class)
        .extracting(ar -> ar.name())
        .isEqualTo("test-actor");
  }

  @Test
  public void shouldSpawnActorWithActorClassWithDefaultMailboxWithDefaultSupervision() {
    final UntypedActorBuilder actorBuilder = new UntypedActorBuilderImpl(
        TestActorBuilderContext.builder()
            .expectedActorNamePattern(() -> compile("^test-actor$"))
            .shoudHaveEmptySupervisionStrategy(true)
            .shoudHaveEmptyMailbox(true)
            .build()
    );

    assertThat(actorBuilder
        .withAbsoluteName("test-actor")
        .withActorClass(TestUntypedActor.class)
        .withArguments("test-actor")
        .create())
        .isNotNull()
        .isInstanceOf(ActorRef.class)
        .extracting(ar -> ar.name())
        .isEqualTo("test-actor");
  }

  @Test
  public void shouldSpawnActorWithSupplierWithCustomMailboxWithDefaultSupervision() {
    final UntypedActorBuilder actorBuilder = new UntypedActorBuilderImpl(
        TestActorBuilderContext.builder()
            .expectedActorNamePattern(() -> compile("^test-actor$"))
            .shoudHaveEmptySupervisionStrategy(true)
            .shoudHaveEmptyMailbox(false)
            .build()
    );

    assertThat(actorBuilder
        .withAbsoluteName("test-actor")
        .withSupplier(() -> TestUntypedActor.builder()
            .tag("test-actor")
            .build())
        .withMailbox(new UnboundedMailbox<>())
        .create())
        .isNotNull()
        .isInstanceOf(ActorRef.class)
        .extracting(ar -> ar.name())
        .isEqualTo("test-actor");
  }

  @Test
  public void shouldSpawnActorWithSupplierWithDefaultMailboxWithDefaultSupervisionWithNamePrefix() {
    final UntypedActorBuilder actorBuilder = new UntypedActorBuilderImpl(
        TestActorBuilderContext.builder()
            .expectedActorNamePattern(() -> compile("^test-actor#[a-f0-9]{32}$"))
            .shoudHaveEmptySupervisionStrategy(true)
            .shoudHaveEmptyMailbox(true)
            .build()
    );

    assertThat(actorBuilder
        .withNamePrefix("test-actor")
        .withSupplier(() -> TestUntypedActor.builder()
            .tag("test-actor")
            .build())
        .create())
        .isNotNull()
        .isInstanceOf(ActorRef.class)
        .extracting(ar -> ar.name())
        .matches(s -> s.startsWith("test-actor#"));
  }

  @Test
  public void shouldSpawnActorWithSupplierWithDefaultMailboxWithCustomSupervision() {
    final UntypedActorBuilder actorBuilder = new UntypedActorBuilderImpl(
        TestActorBuilderContext.builder()
            .expectedActorNamePattern(() -> compile("^test-actor$"))
            .shoudHaveEmptySupervisionStrategy(false)
            .shoudHaveEmptyMailbox(true)
            .build()
    );

    assertThat(actorBuilder
        .withAbsoluteName("test-actor")
        .withSupplier(() -> TestUntypedActor.builder()
            .tag("test-actor")
            .build())
        .withSupervisionStrategy(new SupervisionStrategyInternal() {
          @Override
          public void handleMessageProcessingException(Throwable processingException, ActorInstanceStateTransition transition, ActorInstanceContext context) {

          }

          @Override
          public void handleSignalProcessingException(Throwable signalThrowable, Signal signal, ActorInstanceStateTransition transition, ActorInstanceContext context) {

          }

          @Override
          public void handleActorCreationException(Throwable signalThrowable, ActorInstanceStateTransition transition, ActorInstanceContext context) {

          }
        })
        .create())
        .isNotNull()
        .isInstanceOf(ActorRef.class)
        .extracting(ar -> ar.name())
        .isEqualTo("test-actor");
  }

  @Test
  public void shouldFailWithDuplicateName() {
    final UntypedActorBuilder actorBuilder = new UntypedActorBuilderImpl(
        TestActorBuilderContext.builder()
            .expectedActorNamePattern(() -> compile("^test-actor$"))
            .shoudHaveEmptySupervisionStrategy(true)
            .shoudHaveEmptyMailbox(true)
            .sibling("test-actor")
            .build()
    );

    Assertions.assertThatIllegalArgumentException()
        .isThrownBy(() -> actorBuilder
            .withAbsoluteName("test-actor")
            .withSupplier(() -> TestUntypedActor.builder()
                .tag("test-actor")
                .build())
            .create());
  }

  @Test
  public void shouldFailWithBothActorClassAndSupplier() {
    final UntypedActorBuilder actorBuilder = new UntypedActorBuilderImpl(
        TestActorBuilderContext.builder()
            .expectedActorNamePattern(() -> compile("^test-actor$"))
            .shoudHaveEmptySupervisionStrategy(true)
            .shoudHaveEmptyMailbox(true)
            .build()
    );

    Assertions.assertThatIllegalArgumentException()
        .isThrownBy(() -> actorBuilder
            .withAbsoluteName("test-actor")
            .withSupplier(() -> TestUntypedActor.builder()
                .tag("test-actor")
                .build())
            .withActorClass(TestUntypedActor.class)
            .withArguments("test-actor")
            .create());
  }

  @Test
  public void shouldFailWithNeitherActorClassNorSupplier() {
    final UntypedActorBuilder actorBuilder = new UntypedActorBuilderImpl(
        TestActorBuilderContext.builder()
            .expectedActorNamePattern(() -> compile("^test-actor$"))
            .shoudHaveEmptySupervisionStrategy(true)
            .shoudHaveEmptyMailbox(true)
            .build()
    );

    Assertions.assertThatIllegalArgumentException()
        .isThrownBy(() -> actorBuilder
            .withAbsoluteName("test-actor")
            .withSupplier(() -> TestUntypedActor.builder()
                .tag("test-actor")
                .build())
            .withActorClass(TestUntypedActor.class)
            .withArguments("test-actor")
            .create());
  }

  @Test
  public void shouldFailWithoutName() {
    final UntypedActorBuilder actorBuilder = new UntypedActorBuilderImpl(
        TestActorBuilderContext.builder()
            .shoudHaveEmptySupervisionStrategy(true)
            .shoudHaveEmptyMailbox(true)
            .build()
    );

    Assertions.assertThatIllegalArgumentException()
        .isThrownBy(() -> actorBuilder
            .withSupplier(() -> TestUntypedActor.builder()
                .tag("test-actor")
                .build())
            .create());
  }

  @Test
  public void shouldFailWithBlankName() {
    final UntypedActorBuilder actorBuilder = new UntypedActorBuilderImpl(
        TestActorBuilderContext.builder()
            .shoudHaveEmptySupervisionStrategy(true)
            .shoudHaveEmptyMailbox(true)
            .build()
    );

    Assertions.assertThatIllegalArgumentException()
        .isThrownBy(() -> actorBuilder
            .withAbsoluteName("    ")
            .withSupplier(() -> TestUntypedActor.builder()
                .tag("test-actor")
                .build())
            .create());
  }

  @Test
  public void shouldFailWithEmptyName() {
    final UntypedActorBuilder actorBuilder = new UntypedActorBuilderImpl(
        TestActorBuilderContext.builder()
            .shoudHaveEmptySupervisionStrategy(true)
            .shoudHaveEmptyMailbox(true)
            .build()
    );

    Assertions.assertThatIllegalArgumentException()
        .isThrownBy(() -> actorBuilder
            .withAbsoluteName("")
            .withSupplier(() -> TestUntypedActor.builder()
                .tag("test-actor")
                .build())
            .create());
  }


  @Test
  public void shouldFailWithBlankNamePrefix() {
    final UntypedActorBuilder actorBuilder = new UntypedActorBuilderImpl(
        TestActorBuilderContext.builder()
            .shoudHaveEmptySupervisionStrategy(true)
            .shoudHaveEmptyMailbox(true)
            .build()
    );

    Assertions.assertThatIllegalArgumentException()
        .isThrownBy(() -> actorBuilder
            .withNamePrefix("    ")
            .withSupplier(() -> TestUntypedActor.builder()
                .tag("test-actor")
                .build())
            .create());
  }

  @Test
  public void shouldFailWithEmptyNamePrefix() {
    final UntypedActorBuilder actorBuilder = new UntypedActorBuilderImpl(
        TestActorBuilderContext.builder()
            .shoudHaveEmptySupervisionStrategy(true)
            .shoudHaveEmptyMailbox(true)
            .build()
    );

    Assertions.assertThatIllegalArgumentException()
        .isThrownBy(() -> actorBuilder
            .withNamePrefix("")
            .withSupplier(() -> TestUntypedActor.builder()
                .tag("test-actor")
                .build())
            .create());
  }

  @Test
  public void shouldFailWithUnknownSupervisionStrategy() {
    final UntypedActorBuilder actorBuilder = new UntypedActorBuilderImpl(
        TestActorBuilderContext.builder()
            .expectedActorNamePattern(() -> compile("^test-actor$"))
            .shoudHaveEmptySupervisionStrategy(false)
            .shoudHaveEmptyMailbox(true)
            .build()
    );

    Assertions.assertThatIllegalArgumentException()
        .isThrownBy(() -> actorBuilder
            .withAbsoluteName("test-actor")
            .withSupplier(() -> TestUntypedActor.builder()
                .tag("test-actor")
                .build())
            .withSupervisionStrategy(new SupervisionStrategy() {
            })
            .create());
  }

  @Builder
  @RequiredArgsConstructor
  private static final class TestActorBuilderContext implements ActorBuilderContext<Object> {
    @Builder.Default
    private final boolean shoudHaveEmptyMailbox = false;
    @Builder.Default
    private final boolean shoudHaveEmptySupervisionStrategy = false;

    @Builder.Default
    private final Supplier<Pattern> expectedActorNamePattern = () -> compile("^$");

    @Singular
    private final Set<String> siblings;

    @Builder.Default
    private final Optional<StartupMode> startupMode = empty();

    @Override
    public BiFunction<Class<? extends UntypedActor>, Object[], UntypedActor> defaultInstanceFactory() {
      return (clazz, params) -> {
        if (!(TestUntypedActor.class.isAssignableFrom(clazz))) {
          throw new IllegalArgumentException("Unexpected class passed:" + clazz.getName());
        }
        return TestUntypedActor.builder()
            .tag("default-factory")
            .build();
      };
    }

    @Override
    public ActorRef spawnUntypedActor(final String name,
                                      final Supplier<? extends UntypedActor> supplier,
                                      final Optional<Mailbox> mailbox,
                                      final Optional<SupervisionStrategyInternal> supervisionStrategy,
                                      final Optional<StartupMode> startupMode) {
      if (shoudHaveEmptyMailbox) {
        assertThat(mailbox).isEmpty();
      } else {
        assertThat(mailbox).isNotEmpty();
      }

      if (shoudHaveEmptySupervisionStrategy) {
        assertThat(supervisionStrategy).isEmpty();
      } else {
        assertThat(supervisionStrategy).isNotEmpty();
      }

      if (this.startupMode.isEmpty()) {
        assertThat(this.startupMode.isEmpty()).isEqualTo(startupMode.isEmpty());
      } else {
        assertThat(this.startupMode.get()).isEqualByComparingTo(startupMode.get());
      }

      assertThat(expectedActorNamePattern.get().matcher(name).matches())
          .overridingErrorMessage("expteced match on name pattern '%s', got name '%s'",
              expectedActorNamePattern.get(),
              name)
          .isTrue();

      assertThat(supplier.get()).isInstanceOf(TestUntypedActor.class);

      return new ActorRef<Object>() {

        @Override
        public void tell(Object message, ActorRef sender) {
        }

        @Override
        public CompletionStage<Object> ask(Object message) {
          return null;
        }

        @Override
        public CompletionStage<Object> ask(Object message, Duration timeout) {
          return null;
        }

        @Override
        public String name() {
          return name;
        }
      };
    }

    @Override
    public boolean haveSiblingWithName(final String name) {
      return siblings.contains(name);
    }
  }

  @Builder
  @RequiredArgsConstructor
  @Getter
  private static final class TestUntypedActor implements UntypedActor {
    @Builder.Default
    private final String tag = "default";

    @Override
    public void receive(Object message) {

    }

    @Override
    public void setupContext(ActorContext context) {

    }
  }
}
