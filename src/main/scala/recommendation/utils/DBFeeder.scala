package recommendation.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import recommendation.model._
import recommendation.service.impl.ClientCrud

import scala.collection.mutable
import scala.concurrent.Future
import scala.util.Random

object DBFeeder {

  val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  val mockClients: Map[ClientId, Client] = Map(
    ClientId(0) -> Client(ClientId(0), Regular, Map(LocalDate.parse("01/01/2019", formatter) -> Product(ProductId(0)))),
    ClientId(4) -> Client(ClientId(4), Regular, Map(LocalDate.parse("05/01/2019", formatter) -> Product(ProductId(4)))),
    ClientId(5) -> Client(ClientId(5), Premium, Map(LocalDate.parse("06/01/2019", formatter) -> Product(ProductId(0)),
      LocalDate.parse("23/02/2019", formatter) -> Product(ProductId(1)))),
    ClientId(1) -> Client(ClientId(1), Regular, Map(LocalDate.parse("01/01/2019", formatter) -> Product(ProductId(0)),
      LocalDate.parse("23/02/2019", formatter) -> Product(ProductId(2)),
      LocalDate.parse("13/03/2019", formatter) -> Product(ProductId(3)))),
    ClientId(2) -> Client(ClientId(2), Regular, Map(LocalDate.parse("03/01/2019", formatter) -> Product(ProductId(2)))),
    ClientId(3) -> Client(ClientId(3), Premium, Map(LocalDate.parse("04/07/2019", formatter) -> Product(ProductId(9)),
      LocalDate.parse("23/02/2019", formatter) -> Product(ProductId(1)),
      LocalDate.parse("13/03/2019", formatter) -> Product(ProductId(3)))),
    ClientId(6) -> Client(ClientId(6), Regular, Map(LocalDate.parse("07/07/2019", formatter) -> Product(ProductId(6)))),
  )

  def createClientsRandomly(aValue: Int): Map[ClientId, Client] = {
    val mockClients: mutable.Map[ClientId, Client] = mutable.Map()
    val result: Int = randomNumber(aValue)
    var clientType: ClientType = Regular

    for (i <- 0 until result + 1) {
      if (result % 2 == 0) {
        clientType = Regular
      } else {
        clientType = Premium
      }
      val client = Client(ClientId(i), clientType, createProducts(result))
      mockClients += (ClientId(i) -> client)
    }

    mockClients.toMap
  }

  private def randomNumber(aValue: Int) = {
    val r = new Random()
    val low = 1
    val result = r.nextInt(aValue + 1 - low) + low
    if (result < 0)
      result * (-1)
    else
      result
  }

  def createProducts(value: Int): Map[LocalDate, Product] = {

    var products: mutable.Map[LocalDate, Product] = mutable.Map()

    if (value > 2 && value < 10) {
      products += (LocalDate.parse("0" + value + "/0" + value + "/2019", formatter) -> Product(ProductId(randomNumber(value))))
      products += (LocalDate.parse("0" + (value + 1) + "/0" + (value - 1) + "/2019", formatter) -> Product(ProductId(randomNumber(value))))
    } else if (value > 10 && value < 12) {
      products += (LocalDate.parse(value + "/" + value + "/2019", formatter) -> Product(ProductId(randomNumber(value))))
      products += (LocalDate.parse((value + 1) + "/" + (value - 1) + "/2019", formatter) -> Product(ProductId(randomNumber(value))))
    } else {
      products += (LocalDate.parse("05/05/2019", formatter) -> Product(ProductId(randomNumber(value))))
      products += (LocalDate.parse("05/06/2019", formatter) -> Product(ProductId(randomNumber(value))))
    }
    products.toMap
  }

  def generateHistory(): Future[List[Future[Client]]] = {
    val ourClients: Map[ClientId, Client] = createClientsRandomly(90)
    //    Future.successful(ClientCrud.massCreate(mockClients))
    Future.successful(ClientCrud.massCreate(ourClients))
  }
}
