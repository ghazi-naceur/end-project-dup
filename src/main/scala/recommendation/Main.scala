package recommendation

import recommendation.utils.DBFeeder

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main extends App {

  DBFeeder.generateHistory()

 // Await.result(generateHistory(), Duration.Inf) // C'est mal ! Mais cela nous permet de bloquer pour générer l'historique
  // avant le démarrage du serveur
 // Server.bindAndExpose(ProductRecommendation.recommend)
}