package io.openactors4j.core.impl.boot;

import io.openactors4j.core.boot.ActorSystemFactory;
import java.util.ServiceLoader;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class ActorSystemFactoryImplTest {

  @Test
  public void shouldCreateFactoryThroughServiceLoader() {
    Assertions.assertThat(ServiceLoader.load(ActorSystemFactory.class).findFirst()).isNotEmpty();
  }
}
