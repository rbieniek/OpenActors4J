package io.openactors4j.core.impl.common;

import io.openactors4j.core.common.ActorInstantiationFailureException;
import io.openactors4j.core.untyped.UntypedActor;
import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultActorInstanceFactory<T extends UntypedActor> implements BiFunction<Class<T>, Object[], T> {

  private final Map<Class, PrimitiveTypeHelper<?>> primitiveTypeHelpers = new ConcurrentHashMap<>();

  private final Function<Object, Object> identity = (a) -> a;

  public DefaultActorInstanceFactory() {
    //  boolean, byte, char, short, int, long, float, and double

    primitiveTypeHelpers.put(boolean.class, PrimitiveTypeHelper.<Boolean>builder()
        .boxingClass(Boolean.class)
        .valueMapper(v -> v.booleanValue())
        .build());
    primitiveTypeHelpers.put(byte.class, PrimitiveTypeHelper.<Byte>builder()
        .boxingClass(Byte.class)
        .valueMapper(v -> v.byteValue())
        .build());
    primitiveTypeHelpers.put(char.class, PrimitiveTypeHelper.<Character>builder()
        .boxingClass(Character.class)
        .valueMapper(v -> v.charValue())
        .build());
    primitiveTypeHelpers.put(short.class, PrimitiveTypeHelper.<Short>builder()
        .boxingClass(Short.class)
        .valueMapper(v -> v.shortValue())
        .build());
    primitiveTypeHelpers.put(int.class, PrimitiveTypeHelper.<Integer>builder()
        .boxingClass(Integer.class)
        .valueMapper(v -> v.intValue())
        .build());

    primitiveTypeHelpers.put(long.class, PrimitiveTypeHelper.<Long>builder()
        .boxingClass(Long.class)
        .valueMapper(v -> v.longValue())
        .build());
    primitiveTypeHelpers.put(float.class, PrimitiveTypeHelper.<Float>builder()
        .boxingClass(Float.class)
        .valueMapper(v -> v.floatValue())
        .build());
    primitiveTypeHelpers.put(double.class, PrimitiveTypeHelper.<Double>builder()
        .boxingClass(Double.class)
        .valueMapper(v -> v.doubleValue())
        .build());
  }

  @Override
  @SuppressWarnings( {"PMD.AvoidAccessibilityAlteration", "PMD.PreserveStackTrace", "PMD.AvoidCatchingGenericException"})
  public T apply(final Class<T> actorClass, final Object[] cArgs) {
    final String actorClassName = actorClass.getName();

    try {
      final List<Class> cArgClasses = Arrays.stream(sanitizeObjects(cArgs))
          .map(o -> o.getClass())
          .collect(Collectors.toList());

      log.info("Calling actor instance for class {} with parameter types {}",
          actorClassName,
          cArgClasses.stream()
              .map(c -> c.getName())
              .reduce((a, b) -> String.format("%s,%s", a, b))
              .orElse("none"));

      return AccessController.doPrivileged(new PrivilegedExceptionAction<T>() {
        @Override
        public T run() throws Exception {
          T result;

          if (cArgs != null && cArgs.length > 0) {
            final ConstructorContainer<T> constructorContainer = findMatchingConstructor(actorClass, cArgClasses)
                .orElseThrow(() -> new NoSuchMethodException());

            constructorContainer.getCtor().setAccessible(true);

            final ArrayList<Object> transformedArgs = new ArrayList<>();

            final Iterator<Object> argIt = Arrays.asList(cArgs).iterator();
            final Iterator<Function<Object, Object>> funcIt = constructorContainer.getTransformers().get()
                .stream()
                .map(t -> (Function<Object, Object>) t)
                .iterator();

            while (argIt.hasNext()) {
              transformedArgs.add(funcIt.next().apply(argIt.next()));
            }

            result = constructorContainer.getCtor().newInstance(transformedArgs.toArray(new Object[0]));
          } else {
            final Constructor<T> ctor = actorClass.getDeclaredConstructor();

            ctor.setAccessible(true);

            result = ctor.newInstance();
          }

          return result;
        }
      });
    } catch (PrivilegedActionException e) {
      log.info("Cannot instantiate actor for class {}, reason {}", actorClassName, e, e.getException());

      throw new ActorInstantiationFailureException(e.getException());
    } catch (RuntimeException e) {
      log.info("Cannot instantiate actor for class {}", actorClassName, e);

      throw new ActorInstantiationFailureException(e);
    }
  }

  @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
  private Optional<ConstructorContainer<T>> findMatchingConstructor(final Class<T> clazz, final List<Class> ctorArgClazz) {
    return Arrays.stream(clazz.getDeclaredConstructors())
        .map(ctor -> (Constructor<T>) ctor)
        .map(ctor -> ConstructorContainer.<T>builder().ctor(ctor).transformers(matchCtorArgumens(ctor, ctorArgClazz.iterator())).build())
        .filter(cc -> cc.getTransformers().isPresent())
        .findFirst();
  }

  private Optional<List<Function<?, Object>>> matchCtorArgumens(final Constructor<T> ctor, final Iterator<Class> argIt) {
    boolean canMatch = true;
    final Iterator<Class<?>> ctorIt = Arrays.asList(ctor.getParameterTypes()).iterator();
    final List<Function<?, Object>> transformers = new LinkedList<>();

    while (canMatch && ctorIt.hasNext() && argIt.hasNext()) {
      final Class<?> ctorClazz = ctorIt.next();
      final Class argClazz = argIt.next();

      if (ctorClazz.isPrimitive()) {
        canMatch = evaluatePrimitive(ctorClazz, argClazz)
            .map(func -> transformers.add(func))
            .orElse(false);

      } else if (ctorClazz.isAssignableFrom(argClazz)) {
        transformers.add(identity);
      } else {
        canMatch = false;
      }
    }

    if (ctorIt.hasNext() || argIt.hasNext()) {
      canMatch = false;
    }


    return conditionalReturnTransformers(transformers, canMatch);
  }

  private Optional<Function<?, Object>> evaluatePrimitive(final Class<?> ctorClazz, final Class argClazz) {
    return Optional.ofNullable(primitiveTypeHelpers.get(ctorClazz))
        .filter(helper -> helper.getBoxingClass()
            .isAssignableFrom(argClazz))
        .map(helper -> helper.getValueMapper());
  }

  private Optional<List<Function<?, Object>>> conditionalReturnTransformers(final List<Function<?, Object>> transformer, final boolean shouldPass) {
    return Optional.of(transformer).filter(l -> shouldPass);
  }

  @SuppressWarnings( {"PMD.UseVarargs Priority", "PMD.ConfusingTernary", "PMD.UseVarargs"})
  private Object[] sanitizeObjects(final Object[] args) {
    return args != null ? args : new Object[0];
  }

  @Data
  @Builder
  private static class ConstructorContainer<T> {
    private Constructor<T> ctor;
    private Optional<List<Function<?, Object>>> transformers;
  }

  @Data
  @Builder
  private static class PrimitiveTypeHelper<T> {
    private Class<T> boxingClass;
    private Function<T, Object> valueMapper;
  }
}
