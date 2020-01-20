package io.openactors4j.core.impl.boot;

import io.openactors4j.core.boot.ActorSystemBootstrapConfiguration;
import io.openactors4j.core.boot.ActorSystemFactory;
import io.openactors4j.core.common.ActorSystemBuilder;
import io.openactors4j.core.impl.common.ActorSystemBuilderImpl;

public class ActorSystemFactoryImpl implements ActorSystemFactory {
  @Override
  public ActorSystemBuilder newSystemBuilder(final ActorSystemBootstrapConfiguration bootstrapConfiguration) {
    return new ActorSystemBuilderImpl(bootstrapConfiguration);
  }

  @Override
  public ActorSystemBuilder newSystemBuilder() {
    return new ActorSystemBuilderImpl(ActorSystemBootstrapConfiguration.builder().build());
  }
}
