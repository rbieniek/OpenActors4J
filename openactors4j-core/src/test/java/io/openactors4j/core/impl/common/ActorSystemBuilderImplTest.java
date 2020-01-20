package io.openactors4j.core.impl.common;

import io.openactors4j.core.boot.ActorSystemFactory;
import java.util.ServiceLoader;
import org.assertj.core.api.Assertions;
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
      ActorSystemImpl actorSystem = (ActorSystemImpl)factory.newSystemBuilder().build();

    Assertions.assertThat(actorSystem).isNotNull();
    Assertions.assertThat(actorSystem.name()).isEqualTo("actor-system");
  }
}
