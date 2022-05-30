package com.ludonghua.realtime.dwd

//import org.apache.hadoop.hbase.spark.datasources.HBaseTableCatalog
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

object Test {
  def main(args: Array[String]): Unit = {
    val sparkConf = new SparkConf().setMaster("local[*]")
    val sparkSession = SparkSession.builder().config(sparkConf).getOrCreate()
    import sparkSession.implicits._
    val df = sparkSession.sparkContext.makeRDD(Array(("1", "aa", "33"))).toDF()
    //    val result = df.withColumn("aa", from_unixtime(lit("1605256946851")/1000))
    //    result.show(false)
//    val activityRuleDF = sparkSession.read.options(Map(HBaseTableCatalog.tableCatalog -> activityRuleTable))
//      .format("org.apache.hadoop.hbase.spark")
//      .load().show()
  }

  //  def activityRuleTable = {
  //    s"""{
  //       |"table":{"namespace":"orders", "name":"dwd_activity_rule"},
  //       |"rowkey":"key",
  //       |"columns":{
  //       |"col0":{"cf":"rowkey", "col":"key", "type":"string"},
  //       |"col1":{"cf":"info", "col":"database", "type":"string"},
  //       |"col2":{"cf":"info", "col":"table", "type":"string"},
  //       |"col3":{"cf":"info", "col":"`type`", "type":"float"},
  //       |"col4":{"cf":"info", "col":"ts", "type":"bigint"},
  //       |"col5":{"cf":"info", "col":"id", "type":"int"},
  //       |"col6":{"cf":"info", "col":"activity_id", "type":"int"},
  //       |"col7":{"cf":"info", "col":"activity_type", "type":"int"},
  //       |"col8":{"cf":"info", "col":"condition_amount", "type":"double"},
  //       |"col9":{"cf":"info", "col":"condition_num", "type":"int"},
  //       |"col10":{"cf":"info", "col":"benefit_amount", "type":"double"},
  //       |"col11":{"cf":"info", "col":"benefit_discount", "type":"int"},
  //       |"col12":{"cf":"info", "col":"benefit_level", "type":"int"}
  //       |}
  //       |}""".stripMargin
  //  }
  //
  //  def activityRuleTable = {
  //    s"""{
  //       |"table":{"namespace":"orders", "name":"dwd_activity_rule"},
  //       |"rowkey":"key",
  //       |"columns":{
  //       |"col0":{"cf":"rowkey", "col":"key", "type":"string"},
  //       |"col1":{"cf":"info", "col":"database", "type":"string"},
  //       |"col2":{"cf":"info", "col":"table", "type":"string"},
  //       |"col3":{"cf":"info", "col":"`type`", "type":"float"},
  //       |"col4":{"cf":"info", "col":"ts", "type":"bigint"},
  //       |"col5":{"cf":"info", "col":"id", "type":"int"},
  //       |"col6":{"cf":"info", "col":"activity_id", "type":"int"},
  //       |"col7":{"cf":"info", "col":"activity_type", "type":"int"},
  //       |"col8":{"cf":"info", "col":"condition_amount", "type":"double"},
  //       |"col9":{"cf":"info", "col":"condition_num", "type":"int"},
  //       |"col10":{"cf":"info", "col":"benefit_amount", "type":"double"},
  //       |"col11":{"cf":"info", "col":"benefit_discount", "type":"int"},
  //       |"col12":{"cf":"info", "col":"benefit_level", "type":"int"}
  //       |}
  //       |}""".stripMargin
  //  }
  //
  //  def activitySkuTable: String = {
  //    s"""{
  //       |"table":{"namespace":"orders", "name":"dwd_activity_sku"},
  //       |"rowkey":"key",
  //       |"columns":{
  //       |"col0":{"cf":"rowkey", "col":"key", "type":"string"},
  //       |"col1":{"cf":"info", "col":"database", "type":"string"},
  //       |"col2":{"cf":"info", "col":"table", "type":"string"},
  //       |"col3":{"cf":"info", "col":"`type`", "type":"float"},
  //       |"col4":{"cf":"info", "col":"ts", "type":"bigint"},
  //       |"col5":{"cf":"info", "col":"id", "type":"int"},
  //       |"col6":{"cf":"info", "col":"sku_id", "type":"int"},
  //       |"col7":{"cf":"info", "col":"create_time", "type":"int"}
  //       |}
  //       |}""".stripMargin
  //  }
  //
  //  def couponInfoTable: String = {
  //    s"""{
  //       |"table":{"namespace":"orders", "name":"dwd_coupon_info"},
  //       |"rowkey":"key",
  //       |"columns":{
  //       |"col0":{"cf":"rowkey", "col":"key", "type":"string"},
  //       |"col1":{"cf":"info", "col":"database", "type":"string"},
  //       |"col2":{"cf":"info", "col":"table", "type":"string"},
  //       |"col3":{"cf":"info", "col":"`type`", "type":"float"},
  //       |"col4":{"cf":"info", "col":"ts", "type":"bigint"},
  //       |"col5":{"cf":"info", "col":"id", "type":"int"},
  //       |"col6":{"cf":"info", "col":"coupon_name", "type":"int"},
  //       |"col7":{"cf":"info", "col":"coupon_type", "type":"int"},
  //       |"col8":{"cf":"info", "col":"condition_amount", "type":"double"},
  //       |"col9":{"cf":"info", "col":"activity_id", "type":"int"},
  //       |"col10":{"cf":"info", "col":"benefit_amount", "type":"double"},
  //       |"col11":{"cf":"info", "col":"benefit_discount", "type":"int"},
  //       |"col12":{"cf":"info", "col":"create_time", "type":"bigint"},
  //       |"col13":{"cf":"info", "col":"range_type", "type":"bigint"},
  //       |"col14":{"cf":"info", "col":"limit_num", "type":"int"},
  //       |"col15":{"cf":"info", "col":"taken_count", "type":"int"},
  //       |"col16":{"cf":"info", "col":"start_time", "type":"bigint"},
  //       |"col17":{"cf":"info", "col":"end_time", "type":"bigint"}
  //       |}
  //       |}""".stripMargin
  //  }

}
