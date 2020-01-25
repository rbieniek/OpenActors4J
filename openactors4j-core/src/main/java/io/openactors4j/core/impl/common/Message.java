package io.openactors4j.core.impl.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class Message<T> {
  private final RoutingSlip target;
  private final RoutingSlip sender;
  private final T payload;
  private final Map<String, Object> messageContext = new ConcurrentHashMap<>();
}
