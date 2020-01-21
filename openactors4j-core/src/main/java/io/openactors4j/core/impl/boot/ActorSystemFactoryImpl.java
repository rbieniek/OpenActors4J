package io.openactors4j.core.impl.boot;

import io.openactors4j.core.boot.ActorSystemFactory;
import io.openactors4j.core.common.ActorSystemBuilder;
import io.openactors4j.core.impl.common.ActorSystemBuilderImpl;

public class ActorSystemFactoryImpl implements ActorSystemFactory {
  @Override
  public ActorSystemBuilder newSystemBuilder() {
    return new ActorSystemBuilderImpl();
  }
}
