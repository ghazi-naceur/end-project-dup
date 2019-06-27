package recomendation.engine

import recommendation.engine.ProductRecommendation
import recommendation.model.ClientId
import recommendation.utils.DBFeeder

object RecommendationEngineSpec extends App {

  DBFeeder.generateHistory()
  println(ProductRecommendation.recommend(ClientId(1)))
}
