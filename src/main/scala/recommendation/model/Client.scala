package recommendation.model

import java.time.LocalDate

trait ClientType

final object Regular extends ClientType
final object Premium extends ClientType

case class ClientId(id: Int) extends AnyVal

case class Client(clientId: ClientId, clientType: ClientType, products: Map[LocalDate, Product])