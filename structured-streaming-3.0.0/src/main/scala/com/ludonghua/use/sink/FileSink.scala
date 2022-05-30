package com.ludonghua.use.sink

import org.apache.spark.SparkConf
import org.apache.spark.sql.{Dataset, SparkSession}
import org.apache.spark.sql.streaming.{OutputMode, StreamingQuery}

/**
 * Author Luis
 * DATE 2022-05-29 22:17
 */
object FileSink {
  def main(args: Array[String]): Unit = {
    val sparkConf: SparkConf = new SparkConf()
      .setMaster("local[*]")
      .set("spark.sql.shuffle.partitions", "1")
    val sparkSession: SparkSession = SparkSession.builder().config(sparkConf)
      .getOrCreate()
    import sparkSession.implicits._
    val ds: Dataset[String] = sparkSession.readStream.format("socket")
      .option("host", "hadoop102")
      .option("port", "7777")
      .load()
      .as[String]
    val query: StreamingQuery = ds.writeStream
      .format("json")
      .outputMode(OutputMode.Append())
      .option("checkpointLocation", "./ck")
      .option("path", "./path")
      .start()
    query.awaitTermination()
  }
}
