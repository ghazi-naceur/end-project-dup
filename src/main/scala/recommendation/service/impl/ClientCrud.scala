package recommendation.service.impl

import recommendation.model.{Client, ClientId}
import recommendation.service.Crud

import scala.collection.mutable
import scala.concurrent.Future

object ClientCrud extends Crud[ClientId, Client] {

  var clients: mutable.Map[ClientId, Client] = mutable.Map()

  def create(client: Client): Future[Client] = {
    clients += (client.clientId -> client)
    Future.successful(client)
  }

  def update(clientId: ClientId, client: Client): Future[Client] = {
    clients += (clientId -> client)
    Future.successful(client)
  }

  def read(clientId: ClientId): Future[Option[Client]] = {
    Future.successful(Option(clients(clientId)))
  }

  def delete(clientId: ClientId): Future[Boolean] = {
    if (clients.remove(clientId).isDefined) {
      Future.successful(true)
    } else {
      Future.successful(false)
    }
  }

  def readAll(): Future[mutable.Map[ClientId, Client]] = Future.successful(clients)

  def massCreate(clients: Map[ClientId, Client]): List[Future[Client]] = {
    clients.values.toList.map(create)
  }
}
