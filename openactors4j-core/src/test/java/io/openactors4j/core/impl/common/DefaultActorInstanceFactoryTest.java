package io.openactors4j.core.impl.common;

import static org.assertj.core.api.Assertions.assertThat;


import io.openactors4j.core.common.ActorContext;
import io.openactors4j.core.common.ActorInstantiationFailureException;
import io.openactors4j.core.untyped.UntypedActor;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class DefaultActorInstanceFactoryTest {

  @Test
  public void shouldInstantiateWithPublicDefaultConstructor() {
    ClassWithPublicConstructors instance = new DefaultActorInstanceFactory<ClassWithPublicConstructors>()
        .apply(ClassWithPublicConstructors.class, null);

    assertThat(instance).isNotNull();
    assertThat(instance.getNumber()).isEqualTo(0);
    assertThat(instance.getString()).isNull();
  }

  @Test
  public void shouldInstantiateWithPublicMatchingArguments() {
    ClassWithPublicConstructors instance = new DefaultActorInstanceFactory<ClassWithPublicConstructors>()
        .apply(ClassWithPublicConstructors.class, new Object[] {10, "test"});

    assertThat(instance).isNotNull();
    assertThat(instance.getNumber()).isEqualTo(10);
    assertThat(instance.getString()).isEqualTo("test");
  }

  @Test
  public void shouldNotInstantiateWithPublicMismatchingArguments() {
    Assertions.assertThatExceptionOfType(ActorInstantiationFailureException.class).
        isThrownBy(() -> new DefaultActorInstanceFactory<ClassWithPublicConstructors>()
            .apply(ClassWithPublicConstructors.class, new Object[] {"test", 10}));
  }

  @Test
  public void shouldNotInstantiateWithPublicMissingArguments() {
    Assertions.assertThatExceptionOfType(ActorInstantiationFailureException.class).
        isThrownBy(() -> new DefaultActorInstanceFactory<ClassWithPublicConstructors>()
            .apply(ClassWithPublicConstructors.class, new Object[] {"test"}));
  }

  @Test
  public void shouldNotInstantiateWithPublicSuperflousArguments() {
    Assertions.assertThatExceptionOfType(ActorInstantiationFailureException.class).
        isThrownBy(() -> new DefaultActorInstanceFactory<ClassWithPublicConstructors>()
            .apply(ClassWithPublicConstructors.class, new Object[] {10, "test", 20}));
  }

  @Test
  public void shouldInstantiateWithPrivateDefaultConstructor() {
    ClassWithPrivateConstructors instance = new DefaultActorInstanceFactory<ClassWithPrivateConstructors>()
        .apply(ClassWithPrivateConstructors.class, null);

    assertThat(instance).isNotNull();
    assertThat(instance.getNumber()).isEqualTo(0);
    assertThat(instance.getString()).isNull();
  }

  @Test
  public void shouldInstantiateWithPrivateMatchingArguments() {
    ClassWithPrivateConstructors instance = new DefaultActorInstanceFactory<ClassWithPrivateConstructors>()
        .apply(ClassWithPrivateConstructors.class, new Object[] {10, "test"});

    assertThat(instance).isNotNull();
    assertThat(instance.getNumber()).isEqualTo(10);
    assertThat(instance.getString()).isEqualTo("test");
  }

  @Test
  public void shouldNotInstantiateWithPrivateMismatchingArguments() {
    Assertions.assertThatExceptionOfType(ActorInstantiationFailureException.class).
        isThrownBy(() -> new DefaultActorInstanceFactory<ClassWithPrivateConstructors>()
            .apply(ClassWithPrivateConstructors.class, new Object[] {"test", 10}));
  }

  @Test
  public void shouldNotInstantiateWithPrivateMissingArguments() {
    Assertions.assertThatExceptionOfType(ActorInstantiationFailureException.class).
        isThrownBy(() -> new DefaultActorInstanceFactory<ClassWithPrivateConstructors>()
            .apply(ClassWithPrivateConstructors.class, new Object[] {"test"}));
  }

  @Test
  public void shouldNotInstantiateWithPrivateSuperflousArguments() {
    Assertions.assertThatExceptionOfType(ActorInstantiationFailureException.class).
        isThrownBy(() -> new DefaultActorInstanceFactory<ClassWithPrivateConstructors>()
            .apply(ClassWithPrivateConstructors.class, new Object[] {10, "test", 20}));
  }

  @Test
  public void shouldNotInstantiateWithFailingConstructor() {
    Assertions.assertThatExceptionOfType(ActorInstantiationFailureException.class).
        isThrownBy(() -> new DefaultActorInstanceFactory<ClassWithFailingConstructors>()
            .apply(ClassWithFailingConstructors.class, new Object[] {10, "test", 20}));
  }

  public static class TestUntypedActor implements UntypedActor {
    @Override
    public void setContext(ActorContext context) {

    }

    @Override
    public void receive(Object message) {

    }
  }

  @AllArgsConstructor
  @NoArgsConstructor
  @Getter
  public static class ClassWithPublicConstructors extends TestUntypedActor {
    private int number;
    private String string;
  }

  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  @Getter
  public static class ClassWithPrivateConstructors extends TestUntypedActor {
    private int number;
    private String string;
  }

  @Getter
  public static class ClassWithFailingConstructors extends TestUntypedActor {
    private int value;

    public ClassWithFailingConstructors() {
      value = 1 / 0;
    }
  }
}
