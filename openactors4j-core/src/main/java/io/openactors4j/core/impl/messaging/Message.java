package io.openactors4j.core.impl.messaging;

import io.openactors4j.core.common.SystemAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class Message<T> {
  private final RoutingSlip target;
  private final SystemAddress sender;
  private final T payload;
  private final Map<String, Object> messageContext = new ConcurrentHashMap<>();
}
