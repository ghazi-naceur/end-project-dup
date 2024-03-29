package recommendation.engine

import java.time.LocalDate

import recommendation.model._
import recommendation.service.Crud

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ProductRecommendation(clientRepo: Crud[ClientId, Client]) {

  def recommend(clientId: ClientId): Future[ProductId] = {

    for {
      clients <- clientRepo.readAll()
      currentClient <- clientRepo.read(clientId)
      recommendedProducts <- currentClient match {
        case None =>
          Future.failed(new Exception(
            "Your client id is not found in the database ! Please make sure to provide a valid one."))
        case Some(ourClient) =>
          for {
            remainingClients <- extractRemainingClients(clients, ourClient)
            neighbors <- extractNeighbors(clients, remainingClients, ourClient)
            products <- Future
              .sequence(
                neighbors.map(clientWithProducts =>
                  findFirstNotAlreadyBoughtProduct(clientWithProducts,
                                                   ourClient)))
              .map(_.flatten)
            recommendedProduct <- computeRecommendedProduct(products)
          } yield recommendedProduct
      }
    } yield recommendedProducts

    clientRepo.readAll().flatMap { clients =>
      clientRepo.read(clientId).flatMap {
        case None =>
          Future.failed(new Exception(
            "Your client id is not found in the database ! Please make sure to provide a valid one."))
        case Some(ourClient) =>
          val remainingClients: Future[List[Client]] =
            extractRemainingClients(clients, ourClient)
          for {
            allClients <- clientRepo.readAll()
            clients <- remainingClients
            neighbors <- extractNeighbors(allClients, clients, ourClient)
            products <- Future
              .sequence(neighbors.map(clientWithProducts =>
                findFirstNotAlreadyBoughtProduct(clientWithProducts,
                                                 ourClient)))
              .map(_.flatten)
            recommendedProduct <- computeRecommendedProduct(products)
          } yield recommendedProduct
      }
    }
  }

  private def extractNeighbors(
      allClients: mutable.Map[ClientId, Client],
      remainingClients: List[Client],
      ourClient: Client): Future[List[(ClientId, Set[Product])]] = {
    val listClientsWithOurClientCommonProducts
      : List[(ClientId, Set[Product])] =
      remainingClients.foldLeft(List.empty[(ClientId, Set[Product])]) {
        (clients: List[(ClientId, Set[Product])], client: Client) =>
          val productsInCommon: Set[Product] = client.products.values.toSet
            .intersect(ourClient.products.values.toSet)
          if (productsInCommon.isEmpty) clients
          else (client.clientId, productsInCommon) :: clients
      }

    val neighbors: List[(ClientId, Set[Product])] =
      listClientsWithOurClientCommonProducts
        .sortBy { case (_, products) => - products.size }

      Future { allClients }
      .map(clients => {
        neighbors
          .flatMap(neighbor => {
            clients.get(neighbor._1).collect {
              case neighbor =>
                neighbor.products.toList.map(_ => {
                  val productsWithPurchaseDate: Seq[(LocalDate, Product)] =
                    Seq(neighbor.products.toSeq.sortWith(_._1 isAfter _._1): _*)
                  val prods: Set[Product] =
                    productsWithPurchaseDate.map(_._2).toSet
                  (neighbor.clientId, prods)
                })
            }
          })
          .flatten
      })
  }

  private def extractRemainingClients(
      clients: mutable.Map[ClientId, Client],
      ourClient: Client): Future[List[Client]] = {
    // Setting the remaining clients depending on our client's type
    ourClient.clientType match {
      case Regular => Future(clients.values.filter(_ != ourClient).toList)
      case Premium =>
        Future(
          clients.values
            .filter(c => c != ourClient && c.clientType == Premium)
            .toList)
    }
  }

  private def findFirstNotAlreadyBoughtProduct(
      neighbor: (ClientId, Set[Product]),
      ourClient: Client): Future[Option[Product]] = {
    Future(neighbor._2.find((product: Product) =>
      !ourClient.products.values.toList.contains(product)))
  }

  private def computeRecommendedProduct(
      potentialRecommendations: List[Product]): Future[ProductId] = {
    if (potentialRecommendations.nonEmpty) {
      Future.successful(potentialRecommendations.head.productId)
    } else {
      Future.failed(new Exception("Nothing to recommend !"))
    }
  }
}
