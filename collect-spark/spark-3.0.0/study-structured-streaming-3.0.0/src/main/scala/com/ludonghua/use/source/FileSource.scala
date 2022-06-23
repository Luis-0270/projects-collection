package com.ludonghua.use.source

import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}
import org.apache.spark.sql.streaming.{OutputMode, StreamingQuery}
import org.apache.spark.sql.types.StructType

/**
 * Author Luis
 * DATE 2022-05-29 14:23
 */
object FileSource {
  def main(args: Array[String]): Unit = {
    val sparkConf: SparkConf = new SparkConf()
      .setMaster("local[*]")
      .set("spark.sql.shuffle.partitions", "1")
    val sparkSession: SparkSession = SparkSession.builder().config(sparkConf)
      .getOrCreate()

    import sparkSession.implicits._
    val df: DataFrame = sparkSession.readStream.textFile("C:\\Users\\dell\\Desktop\\test")
      .as[String]
      .flatMap(_.split(",")).groupBy("value").count()
    val query: StreamingQuery = df.writeStream.outputMode(OutputMode.Complete()).format("console")
      .start()
    query.awaitTermination()
  }
}
