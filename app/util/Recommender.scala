package util

import model.AmazonRating
import org.apache.spark.SparkContext
import org.apache.spark.mllib.recommendation.{ALS, Rating}
import org.joda.time.{Seconds, DateTime}
import scala.util.Random

class Recommender(@transient scRr: SparkContext, ratingFile: String) extends Serializable {
  val NumRecommendations = 10
  val MinRecommendationsPerUser = 10
  val MaxRecommendationsPerUser = 20
  val MyUsername = "myself"
  val NumPartitions = 20

  @transient val random = new Random() with Serializable
  
 


 println("Using this ratingFile: " + ratingFile)
 
 val data = scRr.textFile("ratings.csv")
 println(data.first())
 
 // first create an RDD out of the rating file
 val rawTrainingRatings = data.map {
    line =>
      val Array(userId, productId, score) = line.split(",")
      AmazonRating(userId, productId, score.toDouble)
  }
 
   rawTrainingRatings.take(10).foreach(println)
   
  
 println(rawTrainingRatings.count)

 //case class Data_schema(user:string,item:string,rate:Double) 
/* val rawTrainingratings = data.map(_.split(',') match { case Array(userId, productId, score) =>
    AmazonRating(userId, productId, score.toDouble)
  })*/
  

  
  // only keep users that have rated between MinRecommendationsPerUser and MaxRecommendationsPerUser products


val trainingRatings = rawTrainingRatings.groupBy(_.userId)
                                           .filter(r => MinRecommendationsPerUser >= r._2.size  && r._2.size < MaxRecommendationsPerUser)
                                           .flatMap(_._2)
                                           .repartition(NumPartitions)
                                           .cache()



//println("TrainingRatings rdd count: "+trainingRatings.count)

trainingRatings.take(4).foreach(println)
 
 
println(s"Parsed $ratingFile. Kept ${trainingRatings.count()} ratings out of ${rawTrainingRatings.count()}")


  // create user and item dictionaries
  val userDict = new Dictionary(MyUsername +: trainingRatings.map(_.userId).distinct.collect)
  val productDict = new Dictionary(trainingRatings.map(_.productId).distinct.collect)
  
  println("I am after training ratings")

  private def toSparkRating(amazonRating: AmazonRating) = {
    Rating(userDict.getIndex(amazonRating.userId),
      productDict.getIndex(amazonRating.productId),
      amazonRating.rating)
  }
  
  private def toAmazonRating(rating: Rating) = {
    AmazonRating(userDict.getWord(rating.user),
      productDict.getWord(rating.product),
      rating.rating
    )
  }

  // convert to Spark Ratings using the dictionaries
  val sparkRatings = trainingRatings.map(toSparkRating)
  
   println("sparkRatings count:"+sparkRatings.count)
   
   sparkRatings.take(4).foreach(println)
  

  def getRandomProductId = productDict.getWord(random.nextInt(productDict.size))
  
   println("getRandomProductId<===================>"+getRandomProductId)
  

  def predict(ratings: Seq[AmazonRating]) = {
    // train model
    val myRatings = ratings.map(toSparkRating)
    val myRatingRDD = scRr.parallelize(myRatings)
    
//rank=10,itr=20,regularization Parameter = 0.01 ===> ALS alterenate least square method
    val startAls = DateTime.now
    val model = ALS.train((sparkRatings ++ myRatingRDD).repartition(NumPartitions), 10, 20, 0.01)

    val myProducts = myRatings.map(_.product).toSet
    val candidates = scRr.parallelize((0 until productDict.size).filterNot(myProducts.contains))

    // get ratings of all products not in my history ordered by rating (higher first) and only keep the first NumRecommendations
    val myUserId = userDict.getIndex(MyUsername)
    val recommendations = model.predict(candidates.map((myUserId, _))).collect
    val endAls = DateTime.now
    val result = recommendations.sortBy(-_.rating).take(NumRecommendations).map(toAmazonRating)
    val alsTime = Seconds.secondsBetween(startAls, endAls).getSeconds

    println(s"ALS Time: $alsTime seconds")
    result
  }
}
