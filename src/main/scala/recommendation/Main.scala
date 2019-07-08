package recommendation

import recommendation.engine.ProductRecommendation
import recommendation.service.impl.ClientCrud
import recommendation.utils.DBFeeder

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main extends App {

  Await.result(DBFeeder.generateHistory(), Duration.Inf) // C'est mal ! Mais cela nous permet de bloquer pour générer l'historique
  //   avant le démarrage du serveur
  private val recommendationAgent = new ProductRecommendation(ClientCrud)
  Server.bindAndExpose(recommendationAgent.recommend)
}