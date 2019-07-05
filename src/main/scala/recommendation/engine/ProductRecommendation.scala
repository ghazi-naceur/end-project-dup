package recommendation.engine

import java.time.LocalDate

import recommendation.model._
import recommendation.service.Crud
import recommendation.service.impl.ClientCrud

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class ProductRecommendation(clientRepo: Crud[ClientId, Client]) { // should be a class so you can inject your dependencies through the constructor, and be able to test with different Crud implementations (in memory for tests, real DB for prod, etc.)

  def recommend(clientId: ClientId): Future[ProductId] = {

//    val clientRepo: Crud[Client, ClientId] = ClientCrud // No need then
    val clients: mutable.Map[ClientId, Client] =
      Await.result(clientRepo.readAll(), Duration.Inf)

    if (clients.keys.toList.contains(clientId)) { // You should use .get that returns an option and then map or pattern match on it
      val ourClient: Client = clients(clientId)
      val remainingClients: List[Client] =
        extractRemainingClients(clients, ourClient) // Should probably wrap your business logic heavy CPU/mem/IO calls into a Future{...} so you can unblock your main program flow
      val neighbors: Map[ClientId, Set[Product]] =
        extractNeighbors(remainingClients, ourClient)

      if (neighbors.nonEmpty) {
        val orderedNeighbors
          : Map[ClientId, Map[LocalDate, Product]] = // careful here, products are orderdered by date by clients are not ordered by number of similar products with ourClient because Map data structure does not preserve order
          orderNeighbors(neighbors, clients)
        computeRecommendedProduct(orderedNeighbors, ourClient)
      } else {
        Future.failed(new Exception("No neighbors !"))
      }
    } else {
      Future.failed(new Exception(
        "Your client id is not found in the database ! Please make sure to provide a valid one."))
    }
  }

  private def computeRecommendedProduct(
      orderedNeighbors: Map[ClientId, Map[LocalDate, Product]],
      ourClient: Client): Future[ProductId] = {
    val clientsProducts: List[Map[LocalDate, Product]] =
      orderedNeighbors.values.toList
    val recommendedProductsList
      : List[Iterable[Option[Product]]] = ??? // When you have a F[F[X]] after a map, then you should probably gave used flatMap
    /*clientsProducts.map { products: Map[LocalDate, Product] =>
        products.values.map(product =>
          if (!ourClient.products.values.toList.contains(product)) Some(product)
          else None)
      }*/ // => could be simplified by using find

    recommendedProductsList.map(_.toList).collectFirst {
      case p => p.flatten.headOption
    }
    val productsList: List[Option[Product]] =
      recommendedProductsList.collectFirst {
        case p if p.nonEmpty => p.toList
      }.get
    val recommendedProduct: Option[Product] = productsList(0) // at the very least you should use head or headOption

    recommendedProduct match {
      case Some(product) => Future.successful(Some(product.productId).value)
      case None          => Future.failed(new Exception("Nothing to recommend !"))
    }
  }

  private def orderNeighbors(
      neighbors: Map[ClientId, Set[Product]],
      clients: mutable.Map[ClientId, Client]) // should take a crud instead of the full client list
    : Map[ClientId, Map[LocalDate, Product]] = {
    val orderedNeighbors: Map[ClientId, Map[LocalDate, Product]] =
      neighbors.map {
        case (clientId: ClientId, products: Set[Product]) => // case (clientId: ClientId, _)
          val client = clients(clientId) // unsafe, could "not exist", should use .get that returns an Option
          val products =
            Map(client.products.toSeq.sortWith(_._1 isAfter _._1): _*)
          (clientId, products)
      }
    orderedNeighbors
  }

  private def extractNeighbors(
      remainingClients: List[Client],
      ourClient: Client): Map[ClientId, Set[Product]] = {
    // Obtaining the nb of common products between our client and the remaining ones
    val listClientsWithOurClientCommonProducts: List[(ClientId, Set[Product])] =
      remainingClients
        .map(
          client =>
            (client.clientId,
             client.products.values.toSet
               .intersect(ourClient.products.values.toSet)))
        .filter(_._2.nonEmpty) // Could be done in one pass with .foldLeft

    // Sorting neighbors depending on the max number of common products with our client : (ClientId(3),3)
    val neighbors: Map[ClientId, Set[Product]] = Map(
      listClientsWithOurClientCommonProducts
        .sortWith(_._2.size > _._2.size): _*) // Could also be done with .sortBy
    neighbors // Not necessary
  }

  private def extractRemainingClients(clients: mutable.Map[ClientId, Client],
                                      ourClient: Client): List[Client] = {
    // Setting the remaining clients depending on our client's type
    val remainingClients: List[Client] = ourClient.clientType match {
      case Regular => clients.values.filter(_ != ourClient).toList
      case Premium =>
        clients.values
          .filter(c => c != ourClient && c.clientType == Premium)
          .toList
    }
    remainingClients // Not needed if you don't assign a value
  }
}
