package recomendation.engine

import recommendation.engine.ProductRecommendation
import recommendation.model.ClientId
import recommendation.utils.DBFeeder

object ProductRecommendationSpec extends App {

  DBFeeder.generateHistory()
  println(ProductRecommendation.recommend(ClientId(1)))
}
