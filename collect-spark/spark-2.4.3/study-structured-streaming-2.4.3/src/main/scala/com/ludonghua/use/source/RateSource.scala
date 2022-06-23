package com.ludonghua.use.source

import org.apache.spark.sql.{DataFrame, SparkSession}

/**
 * Author Luis
 * DATE 2022-05-26 21:29
 */
/**
 * 以固定的速率生成固定格式的数据, 用来测试 Structured Streaming 的性能
 */
object RateSource {
  def main(args: Array[String]): Unit = {
    val spark: SparkSession = SparkSession
      .builder()
      .master("local[*]")
      .appName("RateSource")
      .getOrCreate()

    val df: DataFrame = spark.readStream
      .format("rate")
      .option("rowsPerSecond", 1000) //设置每秒产生的数据的条数, 默认是 1
      .option("rampUpTime", 1) //设置多少秒到达指定速率 默认为 0
      .option("numPartitions", 3) // 设置分区数  默认是 spark 的默认并行度
      .load

    df.writeStream
      .format("console")
      .outputMode("update")
      .option("truncate", value = false)
      .start
      .awaitTermination()
  }
}
