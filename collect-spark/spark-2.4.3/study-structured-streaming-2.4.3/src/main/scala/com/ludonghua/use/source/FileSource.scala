package com.ludonghua.use.source

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.streaming.Trigger
import org.apache.spark.sql.types.{IntegerType, StringType, StructField, StructType}

/**
 * Author Luis
 * DATE 2022-05-26 20:35
 */
/**
 * 读取目录下的文件
 */
object FileSource {
  def main(args: Array[String]): Unit = {
    val spark: SparkSession = SparkSession
      .builder()
      .master("local[*]")
      .appName("FileSource")
      .getOrCreate()

    import spark.implicits._

    val userSchema: StructType = StructType(StructField("name", StringType) :: StructField("age", IntegerType) :: StructField("sex", StringType) :: Nil)
    val df: DataFrame = spark.readStream
      .format("csv")
      .schema(userSchema)
      .load("C:\\Users\\dell\\Desktop\\ss")
      .groupBy("sex")
      .sum("age")

    df.writeStream
      .format("console")
      .outputMode("update")
      .trigger(Trigger.ProcessingTime(1000))
      .start()
      .awaitTermination()
  }

}
