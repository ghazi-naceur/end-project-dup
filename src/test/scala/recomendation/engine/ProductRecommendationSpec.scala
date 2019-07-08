package recomendation.engine

import recommendation.engine.ProductRecommendation
import recommendation.model.ClientId
import recommendation.service.impl.ClientCrud
import recommendation.utils.DBFeeder

object ProductRecommendationSpec extends App {

  DBFeeder.generateHistory()
  private val recommendationAgent = new ProductRecommendation(ClientCrud)
  println(recommendationAgent.recommend(ClientId(1)))
}
