package io.openactors4j.core.common;

import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class ThreadPoolConfiguration {
  @Builder.Default
  private int minimalDefaultThreadPoolSize = 10;

  @Builder.Default
  private int maximalDefaultThreadPoolSize = 10;

  @Builder.Default
  private int keepaliveTime = 30;

  @Builder.Default
  private TimeUnit timeUnit = TimeUnit.SECONDS;
}
