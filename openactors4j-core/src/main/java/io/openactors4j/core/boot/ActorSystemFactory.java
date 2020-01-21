package io.openactors4j.core.boot;

import io.openactors4j.core.common.ActorSystemBuilder;

public interface ActorSystemFactory {
  ActorSystemBuilder newSystemBuilder();
}
