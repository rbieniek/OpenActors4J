module openactors4j.core {
  requires org.slf4j;
  requires org.apache.commons.lang3;

  requires static lombok;
  requires static org.mapstruct.processor;

  exports io.openactors4j.core.boot;
  exports io.openactors4j.core.common;
  exports io.openactors4j.core.typed;
  exports io.openactors4j.core.spi;
  exports io.openactors4j.core.untyped;

  uses io.openactors4j.core.boot.ActorSystemFactory;

  provides io.openactors4j.core.boot.ActorSystemFactory with io.openactors4j.core.impl.boot.ActorSystemFactoryImpl;
}