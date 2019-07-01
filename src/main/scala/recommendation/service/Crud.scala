package recommendation.service

import recommendation.model.{Client, ClientId, ProductId}

import scala.collection.mutable
import scala.concurrent.Future

// Should be more generic
/*
trait Crud[Entity, EntityId] {
  def create(client: Entity): Future[Entity]
  def update(clientId: EntityId, client: Entity): Future[Entity]
  def read(clientId: EntityId): Future[Entity]
  def delete(clientId: EntityId): Future[Boolean]
  def readAll(): Future[mutable.Map[EntityId, Entity]]
}

object ClientCrud extends Crud[Client, ClientId] {
  override def create(client: Client): Future[Client] = ???

  override def update(clientId: ClientId, client: Client): Future[Client] = ???

  override def read(clientId: ClientId): Future[Client] = ???

  override def delete(clientId: ClientId): Future[Boolean] = ???

  override def readAll(): Future[mutable.Map[ClientId, Client]] = ???
}*/


trait Crud {
  def create(client: Client): Future[Client]
  def update(clientId: ClientId, client: Client): Future[Client]
  def read(clientId: ClientId): Future[Client]
  def delete(clientId: ClientId): Future[Boolean]
  def readAll(): Future[mutable.Map[ClientId, Client]]
}
