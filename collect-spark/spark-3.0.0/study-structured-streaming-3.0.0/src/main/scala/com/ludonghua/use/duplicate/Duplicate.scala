package com.ludonghua.use.duplicate

import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}
import org.apache.spark.sql.streaming.OutputMode

import java.sql.Timestamp

/**
 * Author Luis
 * DATE 2022-05-29 18:59
 */
//1,1001,20,2020-01-01 11:50:00
//1,1002,22,2020-01-01 11:55:00
//2,1003,22,2020-01-01 11:52:00
//3,1004,55,2020-01-01 11:54:00
object Duplicate {
  def main(args: Array[String]): Unit = {
    val sparkConf: SparkConf = new SparkConf()
      .setMaster("local[*]")
      .set("spark.sql.shuffle.partitions", "1")
    val sparkSession: SparkSession = SparkSession.builder().config(sparkConf).getOrCreate()
    val line: DataFrame = sparkSession.readStream.format("socket")
      .option("host", "hadoop102")
      .option("port", "7777")
      .load()
    import sparkSession.implicits._
    val result: Dataset[(String, String, String, Timestamp)] = line.as[String].map(item => {
      val array: Array[String] = item.split(",")
      (array(0), array(1), array(2), Timestamp.valueOf(array(3)))
    }).withWatermark("_4", "2 minutes")
      // watermark过期，丢弃的窗口去重
      .dropDuplicates("_1", "_4")
    result.writeStream.outputMode(OutputMode.Update()).format("console")
      .start().awaitTermination()
  }
}
