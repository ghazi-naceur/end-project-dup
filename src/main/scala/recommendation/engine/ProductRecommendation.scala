package recommendation.engine

import java.time.LocalDate

import recommendation.model._
import recommendation.service.Crud

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ProductRecommendation(clientRepo: Crud[ClientId, Client]) {

  def findFirstNotAlreadyBoughtProduct(neighbor: (ClientId, Set[Product]), ourClient: Client): Future[Option[Product]] =
    Future(neighbor._2.find((product: Product) =>
      !ourClient.products.values.toList.contains(product)))

  def computeRecommendedProduct(potentialRecommendations: List[Product]): Future[ProductId] =
    if (potentialRecommendations.nonEmpty) {
      Future.successful(potentialRecommendations.head.productId)
    } else {
      Future.failed(new Exception("Nothing to recommend !"))
    }

  def recommend(clientId: ClientId): Future[ProductId] = {

    clientRepo.readAll().flatMap { clients =>
      clientRepo.read(clientId).flatMap {
        case None => Future.failed(new Exception("Your client id is not found in the database ! Please make sure to provide a valid one."))
        case Some(ourClient) =>
          val remainingClients: Future[List[Client]] = extractRemainingClients(clients, ourClient)
            for {
              clients <- remainingClients
              neighbors <- extractNeighbors(clients, ourClient)
              products <- Future
                .sequence(neighbors.map(clientWithProducts => findFirstNotAlreadyBoughtProduct(clientWithProducts, ourClient)))
                .map(_.flatten)
              recommendedProduct <- computeRecommendedProduct(products)
            } yield recommendedProduct
      }
    }
  }

  private def extractNeighbors(remainingClients: List[Client], ourClient: Client): Future[List[(ClientId, Set[Product])]] = {
    val listClientsWithOurClientCommonProducts: List[(ClientId, Set[Product])] =
      remainingClients.map(client => (client.clientId, client.products.values.toSet.intersect(ourClient.products.values.toSet)))
        .filter(_._2.nonEmpty) // TODO Could be done in one pass with .foldLeft

    val neighbors: List[(ClientId, Set[Product])] = listClientsWithOurClientCommonProducts.sortBy(_._2.size).reverse // from bigger to smaller

    clientRepo.readAll().map { clients => {
      neighbors.flatMap(neighbor => {
        val client = clients(neighbor._1)
        client.products.toList.map(_ => {
          val productsWithPurchaseDate: Seq[(LocalDate, Product)] = Seq(client.products.toSeq.sortWith(_._1 isAfter _._1): _*)
          val prods: Set[Product] = productsWithPurchaseDate.map(_._2).toSet
          (client.clientId, prods)
        })
      })
    }
    }
  }

  private def extractRemainingClients(clients: mutable.Map[ClientId, Client], ourClient: Client): Future[List[Client]] = {
    // Setting the remaining clients depending on our client's type
    ourClient.clientType match {
      case Regular => Future(clients.values.filter(_ != ourClient).toList)
      case Premium =>
        Future(clients.values
          .filter(c => c != ourClient && c.clientType == Premium)
          .toList)
    }
  }
}
