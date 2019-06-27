package recomendation.engine

import recommendation.engine.RecommendationEngine
import recommendation.model.ClientId
import recommendation.utils.DBFeeder

object RecommendationEngineSpec extends App {

  DBFeeder.generateHistory()
  println(RecommendationEngine.recommend(ClientId(1)))
}
