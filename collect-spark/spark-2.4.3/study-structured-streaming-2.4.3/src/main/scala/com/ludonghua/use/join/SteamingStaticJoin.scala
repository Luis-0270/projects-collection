package com.ludonghua.use.join

import org.apache.spark.sql.{DataFrame, SparkSession}

/**
 * Author Luis
 * DATE 2022-05-27 22:23
 */
object SteamingStaticJoin {
  def main(args: Array[String]): Unit = {
    val spark: SparkSession = SparkSession
      .builder()
      .master("local[*]")
      .appName("SteamingStaticJoin")
      .getOrCreate()

    import spark.implicits._

    // 得到静态的df
    val arr: Array[(String, Int)] = Array(("ls", 20), ("zs", 10), ("ww", 15))
    val staticDF: DataFrame = spark.sparkContext.parallelize(arr).toDF("name", "age")
    // 动态df
    val steamingDF: DataFrame = spark.readStream
      .format("socket")
      .option("host", "hadoop102")
      .option("port", 9999)
      .load
      .as[String]
      .map(line => {
        val splits: Array[String] = line.split(",")
        (splits(0), splits(1))
      })
      .toDF("name", "sex")

    // 内连接
    val joinedDF: DataFrame = steamingDF.join(staticDF, Seq("name"))

    joinedDF.writeStream
      .format("console")
      .outputMode("update")
      .start()
      .awaitTermination()
  }
}
