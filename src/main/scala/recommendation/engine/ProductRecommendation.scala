package recommendation.engine

import java.time.LocalDate

import recommendation.model._
import recommendation.service.impl.ClientCrud

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.control.Breaks


object ProductRecommendation {

  // TODO : Find a way to replace var with val
  var clientsWithOurClientCommonProducts: Map[ClientId, Set[Product]] = Map()
  var highestProductOcc = 0
  var neighborId: ClientId = _
  var remainingClients: List[Client] = List()
  var orderedNeighbors: mutable.Map[ClientId, Map[LocalDate, Product]] = mutable.Map()

  def recommend(clientId: ClientId): Future[ProductId] = {

    val clients: mutable.Map[ClientId, Client] = Await.result(ClientCrud.readAll(), Duration.Inf)

    if (clients.keys.toList.contains(clientId)) {
      // Retrieve our client info
      val ourClient: Client = clients(clientId)

      // Setting the remaining clients depending on our client's type
      if (ourClient.clientType == Regular) {
        remainingClients = clients.values.filter(_ != ourClient).toList
      } else {
        remainingClients = clients.values.filter(c => c != ourClient && c.clientType == Premium).toList
      }

      // Obtaining the nb of common products between our client and the remaining ones
      remainingClients.foreach(c => {
        val commonProducts: Set[Product] = c.products.values.toSet.intersect(ourClient.products.values.toSet)
        if (commonProducts.nonEmpty) {
          clientsWithOurClientCommonProducts += (c.clientId -> commonProducts)
        }
      })
      // Sorting neighbors depending on the max number of common products with our client : (ClientId(3),3)
      val neighbors: Map[ClientId, Set[Product]] = Map(clientsWithOurClientCommonProducts.toSeq.sortWith(_._2.size > _._2.size): _*)

      // Sorting products with the purchase date
      if (neighbors.nonEmpty) {
        for ((k, v) <- neighbors) {
          val client = clients(k)
          val products = Map(client.products.toSeq.sortWith(_._1 isAfter _._1): _*)
          orderedNeighbors += (k -> products)
        }
      } else {
        Future.failed(new Exception("No neighbors !"))
      }

      val loop = new Breaks
      var recommendedProduct: Option[Product] = None

      loop.breakable {
        orderedNeighbors.foreach(neighbor => {
          neighbor._2.values.foreach(product => {
            if (!ourClient.products.values.toList.contains(product)) {
              recommendedProduct = Some(product)
              loop.break()
            }
          })
        })
      }
      if (recommendedProduct.isDefined) {
        Future.successful(Some(recommendedProduct.get.productId).value)
      } else {
        Future.failed(new Exception("Nothing to recommend !"))
      }
    } else {
      Future.failed(new Exception("Your client id is not found in the database ! Please make sure to provide a valid one."))
    }
  }
}
