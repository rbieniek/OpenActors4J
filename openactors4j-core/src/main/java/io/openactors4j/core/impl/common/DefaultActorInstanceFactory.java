package io.openactors4j.core.impl.common;

import io.openactors4j.core.untyped.UntypedActor;
import java.util.function.BiFunction;

public class DefaultActorInstanceFactory<T extends UntypedActor> implements BiFunction<Class<T>, Object[], T> {
  @Override
  public T apply(final Class<T> tClass, final Object[] objects) {
    return null;
  }
}
