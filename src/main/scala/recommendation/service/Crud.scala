package recommendation.service

import scala.collection.mutable
import scala.concurrent.Future

trait Crud[EntityId, Entity] {
  def create(entity: Entity): Future[Entity]

  def update(entityId: EntityId, entity: Entity): Future[Entity]

  def read(entityId: EntityId): Future[Entity]

  def delete(entityId: EntityId): Future[Boolean]

  def readAll(): Future[mutable.Map[EntityId, Entity]]
}