package recommendation

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import recommendation.model.{ClientId, ProductId}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.StdIn
import scala.util.{Failure, Success}

object Server {
  def bindAndExpose(recommend: ClientId => Future[ProductId]): Unit = {
    implicit val system: ActorSystem = ActorSystem("recommendation-engine")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    val route =
      pathPrefix("recommendation" / IntNumber) { id =>
        get {
          onComplete(recommend(ClientId(id))) {
            case Success(reco) =>
              complete(
                HttpEntity(ContentTypes.`text/plain(UTF-8)`, reco.toString))
            case Failure(reason) =>
              failWith(reason)
          }
        }
      }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    println(
      s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}