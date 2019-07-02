package recommendation.engine

import java.time.LocalDate

import recommendation.model._
import recommendation.service.Crud
import recommendation.service.impl.ClientCrud

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object ProductRecommendation {

  def recommend(clientId: ClientId): Future[ProductId] = {

    val clientRepo: Crud[Client, ClientId] = ClientCrud
    val clients: mutable.Map[ClientId, Client] = Await.result(clientRepo.readAll(), Duration.Inf)

    if (clients.keys.toList.contains(clientId)) {
      val ourClient: Client = clients(clientId)
      val remainingClients: List[Client] = extractRemainingClients(clients, ourClient)
      val neighbors: Map[ClientId, Set[Product]] = extractNeighbors(remainingClients, ourClient)

      if (neighbors.nonEmpty) {
        val orderedNeighbors: Map[ClientId, Map[LocalDate, Product]] = orderNeighbors(neighbors, clients)
        computeRecommendedProduct(orderedNeighbors, ourClient)
      } else {
        Future.failed(new Exception("No neighbors !"))
      }
    } else {
      Future.failed(new Exception(
        "Your client id is not found in the database ! Please make sure to provide a valid one."))
    }
  }

  private def computeRecommendedProduct(orderedNeighbors: Map[ClientId, Map[LocalDate, Product]], ourClient: Client): Future[ProductId] = {
    val clientsProducts: List[Map[LocalDate, Product]] = orderedNeighbors.values.toList
    val recommendedProductsList: List[Iterable[Option[Product]]] = clientsProducts.map {
      products: Map[LocalDate, Product] => products.values.map(product => if (!ourClient.products.values.toList.contains(product)) Some(product) else None)
    }
    val productsList: List[Option[Product]] = recommendedProductsList.collectFirst { case p if p.nonEmpty => p.toList }.get
    val recommendedProduct: Option[Product] = productsList(0)

    recommendedProduct match {
      case Some(product) => Future.successful(Some(product.productId).value)
      case None => Future.failed(new Exception("Nothing to recommend !"))
    }
  }

  private def orderNeighbors(neighbors: Map[ClientId, Set[Product]], clients: mutable.Map[ClientId, Client]): Map[ClientId, Map[LocalDate, Product]] = {
    val orderedNeighbors: Map[ClientId, Map[LocalDate, Product]] = neighbors.map { case (clientId: ClientId, products: Set[Product]) =>
      val client = clients(clientId)
      val products = Map(client.products.toSeq.sortWith(_._1 isAfter _._1): _*)
      (clientId, products)
    }
    orderedNeighbors
  }

  private def extractNeighbors(remainingClients: List[Client], ourClient: Client): Map[ClientId, Set[Product]] = {
    // Obtaining the nb of common products between our client and the remaining ones
    val listClientsWithOurClientCommonProducts: List[(ClientId, Set[Product])] = remainingClients
      .map(client => (client.clientId, client.products.values.toSet.intersect(ourClient.products.values.toSet))).filter(_._2.nonEmpty)

    // Sorting neighbors depending on the max number of common products with our client : (ClientId(3),3)
    val neighbors: Map[ClientId, Set[Product]] = Map(listClientsWithOurClientCommonProducts.sortWith(_._2.size > _._2.size): _*)
    neighbors
  }

  private def extractRemainingClients(clients: mutable.Map[ClientId, Client], ourClient: Client): List[Client] = {
    // Setting the remaining clients depending on our client's type
    val remainingClients: List[Client] = ourClient.clientType match {
      case Regular => clients.values.filter(_ != ourClient).toList
      case Premium => clients.values.filter(c => c != ourClient && c.clientType == Premium).toList
    }
    remainingClients
  }
}
