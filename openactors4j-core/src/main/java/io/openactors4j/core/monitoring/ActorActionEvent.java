package io.openactors4j.core.monitoring;

import java.time.Duration;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

@Data
@Builder
@Value
public class ActorActionEvent {
  private String actorName;
  private Duration duration;
  private ActorActionEventType action;
  private ActorOutcomeType outcome;
}
