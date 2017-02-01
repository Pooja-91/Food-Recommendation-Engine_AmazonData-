package util

import java.io.File

import model.AmazonRating

import scala.io.Source

object RatingParser {
  def parse(file: File) = {
    for (line <- Source.fromFile(file).getLines()) yield {
      val Array(itemId, userId, score) = line.split(",")
      AmazonRating(itemId, userId, score.toDouble)
    }
  }
}
