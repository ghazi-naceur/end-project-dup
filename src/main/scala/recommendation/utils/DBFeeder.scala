package recommendation.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import recommendation.model._
import recommendation.service.impl.ClientCrud

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

  def generateHistory(): Unit = {
    // TODO : History generation should be automated and random, not hardcoded
    ClientCrud.massCreate(mockClients)
  }
}
