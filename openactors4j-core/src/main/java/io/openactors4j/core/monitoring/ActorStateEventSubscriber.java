package io.openactors4j.core.monitoring;

import java.util.concurrent.Flow;

public interface ActorStateEventSubscriber extends Flow.Subscriber<ActorStateEvent> {
}
