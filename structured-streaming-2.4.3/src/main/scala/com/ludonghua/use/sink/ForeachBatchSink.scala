package com.ludonghua.use.sink

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.streaming.StreamingQuery

import java.util.Properties

/**
 * Author Luis
 * DATE 2022-05-28 16:48
 */
object ForeachBatchSink {
  def main(args: Array[String]): Unit = {
    val spark: SparkSession = SparkSession
      .builder()
      .master("local[2]")
      .appName("ForeachBatchSink")
      .getOrCreate()

    import spark.implicits._

    val lines: DataFrame = spark.readStream
      .format("socket") // 设置数据源
      .option("host", "hadoop102")
      .option("port", 10000)
      .load

    val wordCount: DataFrame = lines.as[String]
      .flatMap(_.split("\\W+"))
      .groupBy("value")
      .count() // value count

    val props = new Properties()
    props.setProperty("user", "root")
    props.setProperty("password", "123123")

    val query: StreamingQuery = wordCount.writeStream
      .outputMode("complete")
      .foreachBatch((df, batchId) => {
        df.persist()
        df.write.mode("overwrite").jdbc("jdbc:mysql://hadoop102:3306/test?characterEncoding=UTF-8&useSSL=false","word_count", props)
        df.write.mode("overwrite").json("./foreach")
        df.unpersist()
      })
      .start
    query.awaitTermination()
  }
}
