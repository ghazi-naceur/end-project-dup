package recommendation.service.impl

import recommendation.model.{Client, ClientId}
import recommendation.service.Crud

import scala.collection.mutable
import scala.concurrent.Future

object ClientCrud extends Crud{

  var clients: mutable.Map[ClientId, Client] = mutable.Map()

  override def create(client: Client): Future[Client] = {
    clients += (client.clientId -> client)
    Future.successful(client)
  }

  override def update(clientId: ClientId, client: Client): Future[Client] = {
    clients += (clientId -> client)
    Future.successful(client)
  }

  override def read(clientId: ClientId): Future[Client] = {
    Future.successful(clients(clientId))
  }

  override def delete(clientId: ClientId): Future[Boolean] = {
    if(clients.remove(clientId).isDefined) {
      Future.successful(true)
    } else {
      Future.successful(false)
    }
  }

  override def readAll(): Future[mutable.Map[ClientId, Client]] = Future.successful(clients)

  def massCreate(clients: Map[ClientId, Client]): List[Future[Client]] = {
    clients.values.toList.map(create)
  }
}
