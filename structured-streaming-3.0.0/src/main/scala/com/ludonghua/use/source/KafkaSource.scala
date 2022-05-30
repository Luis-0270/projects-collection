package com.ludonghua.use.source

import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.streaming.{OutputMode, StreamingQuery}

/**
 * Author Luis
 * DATE 2022-05-29 14:32
 */
object KafkaSource {
  def main(args: Array[String]): Unit = {
    val sparkConf: SparkConf = new SparkConf()
      .set("spark.sql.shuffle.partitions", "1")
    val sparkSession: SparkSession = SparkSession.builder().config(sparkConf)
      .master("local[*]").getOrCreate()
    val df: DataFrame = sparkSession.readStream.format("kafka")
      .option("kafka.bootstrap.servers", "hadoop102:9092,hadoop103:9092,hadoop104:9092")
      .option("subscribe", "test")
      .option("startingOffsets", "latest")
      .option("maxOffsetsPerTrigger", "2000")
      .option("kafka.group.id", "test")
      .load()
    import sparkSession.implicits._
    val query: StreamingQuery = df.selectExpr("CAST(value AS STRING)").as[String]
      .flatMap(_.split(" ")).groupBy("value").count()
      .writeStream //.option("checkpointLocation","./ck1")
      .outputMode(OutputMode.Update()).format("console").start()
    query.awaitTermination()
  }
}
