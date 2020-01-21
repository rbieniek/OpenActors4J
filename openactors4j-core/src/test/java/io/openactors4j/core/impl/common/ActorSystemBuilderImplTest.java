package io.openactors4j.core.impl.common;

import static org.assertj.core.api.Assertions.assertThat;


import io.openactors4j.core.boot.ActorSystemFactory;
import io.openactors4j.core.common.ThreadPoolConfiguration;
import java.util.ServiceLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ActorSystemBuilderImplTest {

  private static ActorSystemFactory factory;

  @BeforeAll
  public static void obtainActorSystemFactory() {
    factory = ServiceLoader.load(ActorSystemFactory.class).findFirst().get();
  }

  @Test
  public void shouldCreateActorSystemWithDefaults() {
    try (ActorSystemImpl actorSystem = (ActorSystemImpl) factory.newSystemBuilder().build()) {
      assertThat(actorSystem).isNotNull();
      assertThat(actorSystem.name()).isEqualTo("actor-system");
      assertThat(actorSystem.getSystemThreadPoolConfiguration())
          .isEqualTo(ThreadPoolConfiguration.builder()
              .build());
      assertThat(actorSystem.getUserThreadPoolConfiguration())
          .isEqualTo(ThreadPoolConfiguration.builder()
              .build());
    }
  }
}
