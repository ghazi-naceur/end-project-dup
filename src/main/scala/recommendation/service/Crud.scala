package recommendation.service

import recommendation.model.{Client, ClientId}

import scala.collection.mutable
import scala.concurrent.Future

trait Crud {
  def create(client: Client): Future[Client]
  def update(clientId: ClientId, client: Client): Future[Client]
  def read(clientId: ClientId): Future[Client]
  def delete(clientId: ClientId): Future[Boolean]
  def readAll(): Future[mutable.Map[ClientId, Client]]
}
