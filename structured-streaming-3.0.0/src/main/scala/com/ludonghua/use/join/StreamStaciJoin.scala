package com.ludonghua.use.join

import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}
import org.apache.spark.sql.streaming.OutputMode

/**
 * Author Luis
 * DATE 2022-05-29 19:16
 */
object StreamStaciJoin {
  def main(args: Array[String]): Unit = {
    val sparkConf: SparkConf = new SparkConf()
      .setMaster("local[*]")
      .set("spark.sql.shuffle.partitions", "1")
    val sparkSession: SparkSession = SparkSession.builder().config(sparkConf).getOrCreate()
    import sparkSession.implicits._
    val df: DataFrame = sparkSession.read.csv("C:\\Users\\dell\\Desktop\\test\\test.csv")
      .toDF("uid", "name", "age")
    val dstream: Dataset[String] = sparkSession.readStream.format("socket")
      .option("host", "hadoop102")
      .option("port", "7777")
      .load()
      .as[String]
    val dfStudent: DataFrame = dstream.map(item => {
      val array: Array[String] = item.split(",")
      (array(0), array(1))
    }).toDF("uid", "school")
    val result: DataFrame = dfStudent.join(df, Seq("uid"))
    result.writeStream.outputMode(OutputMode.Update()).format("console")
      .start().awaitTermination()
  }
}
