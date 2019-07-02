package recommendation.service

import scala.collection.mutable
import scala.concurrent.Future

trait Crud[Entity, EntityId] {
  def create(client: Entity): Future[Entity]

  def update(clientId: EntityId, client: Entity): Future[Entity]

  def read(clientId: EntityId): Future[Entity]

  def delete(clientId: EntityId): Future[Boolean]

  def readAll(): Future[mutable.Map[EntityId, Entity]]
}