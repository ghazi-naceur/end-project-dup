package recommendation.engine

import java.time.LocalDate

import recommendation.model._
import recommendation.service.Crud

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class ProductRecommendation(clientRepo: Crud[ClientId, Client]) {

  def recommend(clientId: ClientId): Future[ProductId] = {

    val clients: mutable.Map[ClientId, Client] = Await.result(
      clientRepo.readAll(),
      Duration.Inf) // Cannot be done in real world app, cannot scale, you should use the repo instead

    clients.get(clientId) match {
      case None =>
        Future.failed(new Exception(
          "Your client id is not found in the database ! Please make sure to provide a valid one."))
      case Some(ourClient) =>
        val remainingClients: Future[List[Client]] = Future {
          extractRemainingClients(clients, ourClient)
        }
        val futureNeighbors: Future[List[(ClientId, Set[Product])]] =
          extractNeighbors(remainingClients, ourClient)

        val neighbors: List[(ClientId, Set[Product])] =
          Await.result(futureNeighbors, Duration.Inf) // Forbidden

        def findFirstNotAlreadyBoughtProduct(
            neighbor: (ClientId, Set[Product])): Future[Option[Product]] =
          Future(neighbor._2.find((product: Product) =>
            !ourClient.products.values.toList.contains(product)))

        def computeRecommendedProduct(
            potentialRecommendations: List[Product]): Future[ProductId] =
          if (potentialRecommendations.nonEmpty) {
            Future.successful(potentialRecommendations.head.productId)
          } else {
            Future.failed(new Exception("Nothing to recommend !"))
          }

        remainingClients
          .flatMap(extractNeighbors(_, ourClient)) // Future[List[...]]
          .flatMap(neighbors => // List[A]
            Future.sequence(neighbors.map(findFirstNotAlreadyBoughtProduct))) // f: A => Future[B] --- List[A].map(f) => List[Future[B]] --- Future[List[B]]
          .map(_.flatten) //
          .flatMap(computeRecommendedProduct)

        for {
          clients <- remainingClients
          neighbors <- extractNeighbors(clients, ourClient)
          products <- Future
            .sequence(neighbors.map(findFirstNotAlreadyBoughtProduct))
            .map(_.flatten)
          recommendedProduct <- computeRecommendedProduct(products)
        } yield recommendedProduct

        /*
        Old versions with functions returning synchronous "simple" return types instead of as asynchronous "boxed" types
        remainingClients
          .map(extractNeighbors(_, ourClient)) // same as .map(c => extractNeighbors(c, ourClient))
          .map(_.flatMap(findFirstNotAlreadyBoughtProduct))
          .flatMap(computeRecommendedProduct) // same as .flatMap(products => eventualId(products))*/

        val potentialRecommendations: List[Product] =
          neighbors.flatMap(neighbor =>
            neighbor._2.find((product: Product) =>
              !ourClient.products.values.toList.contains(product)))
        if (potentialRecommendations.nonEmpty) {
          Future.successful(potentialRecommendations.head.productId)
        } else {
          Future.failed(new Exception("Nothing to recommend !"))
        }
    }
  }

  private def extractNeighbors(
      futureRemainingClients: List[Client], // Your business logic should not be dependent on the "context" of the values it works on
      ourClient: Client): Future[List[(ClientId, Set[Product])]] = Future {

    val remainingClients = Await.result(futureRemainingClients, Duration.Inf)

    val listClientsWithOurClientCommonProducts: List[(ClientId, Set[Product])] =
      remainingClients
        .map(
          client =>
            (client.clientId,
             client.products.values.toSet
               .intersect(ourClient.products.values.toSet)))
        .filter(_._2.nonEmpty) // TODO Could be done in one pass with .foldLeft

    val neighbors: List[(ClientId, Set[Product])] =
      listClientsWithOurClientCommonProducts
        .sortBy(_._2.size)
        .reverse // from bigger to smaller
    val clients: mutable.Map[ClientId, Client] =
      Await.result(clientRepo.readAll(), Duration.Inf)
    neighbors.flatMap(neighbor => {
      val client = clients(neighbor._1)
      client.products.toList.map(_ => {
        val productsWithPurchaseDate: Seq[(LocalDate, Product)] =
          Seq(client.products.toSeq.sortWith(_._1 isAfter _._1): _*)
        val prods: Set[Product] = productsWithPurchaseDate.map(_._2).toSet
        (client.clientId, prods)
      })
    })
  }

  private def extractRemainingClients(
      clients: mutable.Map[ClientId, Client],
      ourClient: Client): Future[List[Client]] = {
    // Setting the remaining clients depending on our client's type
    ourClient.clientType match {
      case Regular => clients.values.filter(_ != ourClient).toList
      case Premium =>
        clients.values
          .filter(c => c != ourClient && c.clientType == Premium)
          .toList
    }
  }
}
