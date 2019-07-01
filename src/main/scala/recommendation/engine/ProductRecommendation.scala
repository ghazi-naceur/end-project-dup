package recommendation.engine

import java.time.LocalDate

import recommendation.model._
import recommendation.service.Crud
import recommendation.service.impl.ClientCrud

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.control.Breaks

object ProductRecommendation {

  // TODO : Find a way to replace var with val
  // No vars, no mutations
  var clientsWithOurClientCommonProducts: Map[ClientId, Set[Product]] = Map()
  var highestProductOcc = 0
  var neighborId: ClientId = _
  var remainingClients: List[Client] = List()
  var orderedNeighbors: mutable.Map[ClientId, Map[LocalDate, Product]] =
    mutable.Map()

  // Should be split a lot
  def recommend(clientId: ClientId): Future[ProductId] = {

    // Should be a repository (ClientCrud) not a Map
    val clients: mutable.Map[ClientId, Client] =
      Await.result(ClientCrud.readAll(), Duration.Inf)
    /*val clientRep: Crud = ???

    clientRep.read(clientId...)*/

    if (clients.keys.toList.contains(clientId)) {
      // Retrieve our client info
      val ourClient: Client = clients(clientId)

      // Setting the remaining clients depending on our client's type
      // Should use pattern matching
      /*ourClient.clientId match {
        case Regular => ...
      }*/
      if (ourClient.clientType == Regular) {
        remainingClients = clients.values.filter(_ != ourClient).toList
      } else {
        remainingClients = clients.values
          .filter(c => c != ourClient && c.clientType == Premium)
          .toList
      }

      // Obtaining the nb of common products between our client and the remaining ones
      // Foreach is only for debug purpose
      // Maybe you should return a clientsWithOurClientCommonProductsList ?
      /*val clientsWithOurClientCommonProducts = List(???).map(f)*/
      remainingClients.foreach(c => {
        val commonProducts: Set[Product] =
          c.products.values.toSet.intersect(ourClient.products.values.toSet)
        if (commonProducts.nonEmpty) {
          clientsWithOurClientCommonProducts += (c.clientId -> commonProducts)
        }
      })
      // Sorting neighbors depending on the max number of common products with our client : (ClientId(3),3)
      val neighbors: Map[ClientId, Set[Product]] = Map(
        clientsWithOurClientCommonProducts.toSeq
          .sortWith(_._2.size > _._2.size): _*)

      // Sorting products with the purchase date
      if (neighbors.nonEmpty) {
        // No explicit loops in Scala
        /*neighbors.map((clientId, products) => ???)*/
        for ((k, v) <- neighbors) {
          val client = clients(k)
          val products = Map(
            client.products.toSeq.sortWith(_._1 isAfter _._1): _*)
          orderedNeighbors += (k -> products)
        }
      } else {
        Future.failed(new Exception("No neighbors !"))
      }

      val loop = new Breaks
      var recommendedProduct: Option[Product] = None

      // No loops, no break => find / collectFirst / ...
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
      // pattern matching ?
      /*val opt: Option[Int] = ???
      opt match {
        case Some(value) => // Do something with "value"
        case None        => // Do something if there is no content
      }*/

      if (recommendedProduct.isDefined) {
        Future.successful(Some(recommendedProduct.get.productId).value)
      } else {
        Future.failed(new Exception("Nothing to recommend !"))
      }
    } else {
      Future.failed(new Exception(
        "Your client id is not found in the database ! Please make sure to provide a valid one."))
    }
  }
}
