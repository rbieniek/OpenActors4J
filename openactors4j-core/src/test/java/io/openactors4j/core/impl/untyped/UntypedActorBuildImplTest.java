package io.openactors4j.core.impl.untyped;

import static org.assertj.core.api.Assertions.assertThat;


import io.openactors4j.core.common.ActorContext;
import io.openactors4j.core.common.Mailbox;
import io.openactors4j.core.common.SupervisionStrategy;
import io.openactors4j.core.impl.system.ActorBuilderContext;
import io.openactors4j.core.untyped.UntypedActor;
import io.openactors4j.core.untyped.UntypedActorBuilder;
import io.openactors4j.core.untyped.UntypedActorRef;
import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

public class UntypedActorBuildImplTest {

  @Test
  public void shouldSpawnActorWithDefaultMailboxWithDefaultSupervision() {
    final UntypedActorBuilder actorBuilder = new UntypedActorBuilderImpl(
        TestActorBuilderContext.builder()
            .expectedActorNamePattern(() -> Pattern.compile("^test-actor$"))
            .shoudHaveEmptySupervisionStrategy(true)
            .shoudHaveEmptyMailbox(true).build()
    );

    assertThat(actorBuilder
        .withAbsoluteName("test-actor")
        .create())
        .isNotNull()
        .isInstanceOf(UntypedActorRef.class)
        .extracting(ar -> ar.name())
        .isEqualTo("test-actor");
  }

  @Builder
  @RequiredArgsConstructor
  private static final class TestActorBuilderContext implements ActorBuilderContext {
    @Builder.Default
    private final boolean shoudHaveEmptyMailbox = false;
    @Builder.Default
    private final boolean shoudHaveEmptySupervisionStrategy = false;

    @Builder.Default
    private final Supplier<Pattern> expectedActorNamePattern = () -> Pattern.compile("^$");

    @Builder.Default
    private Set<String> siblings = Collections.emptySet();

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
    public UntypedActorRef spawnUntypedActor(String name, Supplier<? extends UntypedActor> supplier, Optional<Mailbox> mailbox, Optional<SupervisionStrategy> supervisionStrategy) {
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

      assertThat(expectedActorNamePattern.get().matcher(name).matches()).isTrue();

      assertThat(supplier.get()).isInstanceOf(TestUntypedActor.class);

      return new UntypedActorRef() {
        @Override
        public UntypedActorRef tell(Object message, UntypedActorRef sender) {
          return null;
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
          return null;
        }
      };
    }

    @Override
    public boolean haveSiblingWithName(String name) {
      return false;
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
    public void setContext(ActorContext context) {

    }
  }
}
