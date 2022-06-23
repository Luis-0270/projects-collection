package com.ludonghua.use.window

import org.apache.spark.SparkConf
import org.apache.spark.sql.streaming.OutputMode
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}

import java.sql.Timestamp

/**
 * Author Luis
 * DATE 2022-05-29 16:19
 */

// 1001,2020-08-06 16:08:54
//1002,2020-08-06 16:05:57
//1003,2020-08-06 16:08:58
//1004,2020-08-06 16:07:52 延迟数据
object window1 {

  case class Student(uid: Int, time: Timestamp)

  def main(args: Array[String]): Unit = {
    val sparkConf: SparkConf = new SparkConf()
      .setMaster("local[*]")
      .set("spark.sql.shuffle.partitions", "1")
    val sparkSession: SparkSession = SparkSession.builder().config(sparkConf)
      .getOrCreate()
    import sparkSession.implicits._
    val ds: Dataset[Student] = sparkSession.readStream.format("socket")
      .option("host", "hadoop102")
      .option("port", "7777")
      .load()
      .as[String]
      .map(item => {
        val array: Array[String] = item.split(",")
        Student(array(0).toInt, Timestamp.valueOf(array(1)))
      })

    import org.apache.spark.sql.functions._

    val result: DataFrame = ds.groupBy(window(col("time"), "10 minutes", "5 minutes")).count()
    result.writeStream
      .outputMode(OutputMode.Update())
      .format("console")
      .option("truncate", "false")
      .start()
      .awaitTermination()
  }
}
