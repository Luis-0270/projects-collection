package com.ludonghua.use.option

import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}
import org.apache.spark.sql.types.{LongType, StringType, StructType}

/**
 * Author Luis
 * DATE 2022-05-26 21:57
 */
/**
 * 强类型 api
 */
object TypeOpt {
  def main(args: Array[String]): Unit = {
    val spark: SparkSession = SparkSession
      .builder()
      .master("local[*]")
      .appName("BasicOperation")
      .getOrCreate()

    import spark.implicits._

    val peopleSchema: StructType = new StructType()
      .add("name", StringType)
      .add("age", LongType)
      .add("sex", StringType)
    val peopleDF: DataFrame = spark.readStream
      .schema(peopleSchema)
      .json("C:\\Users\\dell\\Desktop\\ss\\json")  // 等价于: format("json").load(path)

    val ds: Dataset[String] = peopleDF.as[People].filter(_.age > 20).map(_.name)

    ds.writeStream
      .outputMode("update")
      .format("console")
      .start
      .awaitTermination()
  }
}

case class People(name: String, age: Long, sex: String)
