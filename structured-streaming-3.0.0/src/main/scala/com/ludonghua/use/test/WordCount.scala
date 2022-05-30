package com.ludonghua.use.test

import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}
import org.apache.spark.sql.streaming.{OutputMode, StreamingQuery, Trigger}

import java.util.concurrent.TimeUnit

/**
 * Author Luis
 * DATE 2022-05-29 11:11
 */
object WordCount {
  def main(args: Array[String]): Unit = {
    val sparkConf: SparkConf = new SparkConf()
      .setMaster("local[*]")
      .set("spark.sql.shuffle.partitions", "1")
    val sparkSession: SparkSession = SparkSession.builder().config(sparkConf)
      .getOrCreate()
    import sparkSession.implicits._
    val df: Dataset[String] = sparkSession.readStream.format("socket")
      .option("host", "hadoop102")
      .option("port", "7777")
      .load()
      .as[String]
    val result: DataFrame = df.flatMap(_.split(" ")).groupBy("value").count()
    val query: StreamingQuery = result.writeStream.outputMode(OutputMode.Update())
      .trigger(Trigger.ProcessingTime(1, TimeUnit.MINUTES))
      .format("console")
      .start()
    query.awaitTermination()
  }
}
