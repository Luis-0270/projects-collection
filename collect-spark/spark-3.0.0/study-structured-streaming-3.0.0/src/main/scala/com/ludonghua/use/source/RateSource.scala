package com.ludonghua.use.source

import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SparkSession}

/**
 * Author Luis
 * DATE 2022-05-29 16:07
 */
object RateSource {
  def main(args: Array[String]): Unit = {
    val sparkConf: SparkConf = new SparkConf()
      .set("spark.sql.shuffle.partitions", "1")
    val sparkSession: SparkSession = SparkSession.builder().config(sparkConf)
      .master("local[*]").getOrCreate()
    val rows: DataFrame = sparkSession.readStream.format("rate")
      .option("rowsPerSecond", 10) // 设置每秒产生的数据的条数, 默认是 1
      .option("rampUpTime", 1) // 设置多少秒到达指定速率 默认为 0
      .option("numPartitions", 1) /// 设置分区数  默认是 spark 的默认并行度
      .load
    rows.writeStream.outputMode("update")
      .format("console")
      .start()
      .awaitTermination()

  }
}
