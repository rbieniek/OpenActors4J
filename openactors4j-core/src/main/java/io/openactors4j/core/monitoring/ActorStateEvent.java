package io.openactors4j.core.monitoring;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

@Data
@Builder
@Value
public class ActorStateEvent {
  private String actorName;
  private ActorStateEventType event;
}
