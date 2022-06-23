package com.ludonghua.use.option

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.types.{LongType, StringType, StructType}

/**
 * Author Luis
 * DATE 2022-05-26 21:35
 */
/**
 * 弱类型 api
 */
object UnTypeOpt {
  def main(args: Array[String]): Unit = {
    val spark: SparkSession = SparkSession
      .builder()
      .master("local[*]")
      .appName("BasicOperation")
      .getOrCreate()

    val peopleSchema: StructType = new StructType()
      .add("name", StringType)
      .add("age", LongType)
      .add("sex", StringType)

    val peopleDF: DataFrame = spark.readStream
      .schema(peopleSchema)
      .json("C:\\Users\\dell\\Desktop\\ss\\json")  // 等价于: format("json").load(path)


    val df: DataFrame = peopleDF.select("name", "age", "sex").where("age > 20").groupBy("sex").sum("age") // 弱类型 api
    df.writeStream
      .outputMode("complete")
      .format("console")
      .start
      .awaitTermination()
  }
}
