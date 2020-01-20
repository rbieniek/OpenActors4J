module openactors4j.core {
  requires org.slf4j;
  
  requires static lombok;
  requires static org.mapstruct.processor;

  exports io.openactors4j.core.boot;
  exports io.openactors4j.core.common;
  exports io.openactors4j.core.typed;
  exports io.openactors4j.core.untyped;

  provides io.openactors4j.core.boot.ActorSystemFactory with io.openactors4j.core.impl.boot.ActorSystemFactoryImpl;
}