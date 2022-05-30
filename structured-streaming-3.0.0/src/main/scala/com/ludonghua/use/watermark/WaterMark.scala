package com.ludonghua.use.watermark

import org.apache.spark.SparkConf
import org.apache.spark.sql.streaming.OutputMode
import org.apache.spark.sql.{DataFrame, SparkSession}

import java.sql.Timestamp

/**
 * Author Luis
 * DATE 2022-05-29 16:33
 */

//2020-08-06 16:08:54,dog
//2020-08-06 16:05:57,dog
//2020-08-06 16:08:58,dog
//2020-08-06 16:15:52,dog
object WaterMark {
  def main(args: Array[String]): Unit = {
    val sparkConf: SparkConf = new SparkConf()
      .setMaster("local[*]")
      .set("spark.sql.shuffle.partitions", "1")
    val sparkSession: SparkSession = SparkSession.builder().config(sparkConf)
      .getOrCreate()
    import sparkSession.implicits._
    val df: DataFrame = sparkSession.readStream.format("socket")
      .option("host", "hadoop102")
      .option("port", "7777")
      .load()
      .as[String]
      .map(item => {
        val array: Array[String] = item.split(",")
        (Timestamp.valueOf(array(0)), array(1))
      }).toDF("time", "name")

    import org.apache.spark.sql.functions._

    val result: DataFrame = df.withWatermark("time", "2 minutes")
      .groupBy(window(col("time"), "10 minutes", "2 minutes")).count()
    result.writeStream
      .outputMode(OutputMode.Update())
      .format("console")
      .option("truncate", "false")
      .start()
      .awaitTermination()
  }
}
