package com.ludonghua.use.window

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.streaming.Trigger

/**
 * Author Luis
 * DATE 2022-05-27 15:35
 */
object Window2 {
  def main(args: Array[String]): Unit = {
    val spark: SparkSession = SparkSession
      .builder()
      .master("local[*]")
      .appName("Window2")
      .getOrCreate()

    import org.apache.spark.sql.functions._
    import spark.implicits._

    spark.readStream
      .format("socket")
      .option("host", "hadoop102")
      .option("port", 9999)
      .load()
      .as[String]
      .map(line => {
        val split: Array[String] = line.split(",")
        (split(0), split(1))
      })
      .toDF("ts", "word")
      .groupBy(
        window($"ts", "10 minutes", "3 minutes"),
        $"word"
      )
      .count()
      .sort("window")
      .writeStream
      .format("console")
      .outputMode("complete")
      .trigger(Trigger.ProcessingTime(2000))
      .option("truncate", value = false)
      .start
      .awaitTermination()
  }
}
