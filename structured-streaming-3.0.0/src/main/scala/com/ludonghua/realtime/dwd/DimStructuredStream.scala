package com.ludonghua.realtime.dwd

import com.alibaba.fastjson.serializer.SerializerFeature
import com.alibaba.fastjson.{JSON, JSONObject}
import com.ludonghua.producer.{Coupon, OrderDeatails, OrderMain, PreferentialActivities}
import com.ludonghua.realtime.utils.Utils
import org.apache.hadoop.hbase.client.{Get, Put, Table}
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.{CellUtil, TableName}
import org.apache.spark.SparkConf
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.sql.{Dataset, SparkSession}

import java.util
import scala.beans.BeanProperty

//spark-submit --master yarn --deploy-mode client --driver-memory 1g  --num-executors 3 --executor-cores 3   --executor-memory 4g --queue spark --class com.atguigu.dwd.stream.DimStructuredStream  Structured-Streaming-1.0-SNAPSHOT-jar-with-dependencies.jar
object DimStructuredStream {
  val orderMaingGroupid = "orders_dim_stream_groupid_orderMain"
  val orderDetalGroupid = "orders_dim_stream_groupid_orderDetail"
  val preferentialGroupid = "orders_dim_stream_groupid_preferential"
  val couponGroupid = "orders_dim_stream_groupid_coupon"
  val bootStrapServers = "hadoop101:9092,hadoop102:9092,hadoop103:9092"

  def main(args: Array[String]): Unit = {
    System.setProperty("HADOOP_USER_NAME", "root")
    val sparkConf = new SparkConf().setAppName("dimOrderStream") //.setMaster("local[*]")
      .set("spark.sql.shuffle.partitions", "12")
    val sparkSession = SparkSession.builder().config(sparkConf).getOrCreate()
    val sparkContext = sparkSession.sparkContext
    import org.apache.spark.sql.functions._
    import sparkSession.implicits._
    val orderMainDStream = getDStream(bootStrapServers, "order_main", "earliest", orderMaingGroupid, "4800", sparkSession)
      .as[String].map(item => {
      JSON.parseObject[OrderMain](item, classOf[OrderMain])
    }).withColumn("create_time", to_timestamp(from_unixtime(col("create_time") / 1000)))
      .withColumnRenamed("create_time", "orderMain_create_time")
      .withColumnRenamed("operate_time", "orderMain_operte_time")
      .withColumnRenamed("table", "orderMain_table")
      .withColumnRenamed("img_url", "orderMain_img_url")
      .withWatermark("orderMain_create_time", "2 hours")

    val orderDetailDStream = getDStream(bootStrapServers, "order_details", "earliest", orderDetalGroupid, "4800", sparkSession)
      .as[String].map(item => {
      JSON.parseObject[OrderDeatails](item, classOf[OrderDeatails])
    }).withColumn("create_time", to_timestamp(from_unixtime(col("create_time") / 1000)))
      .withColumnRenamed("id", "orderDetail_id")
      .withColumnRenamed("order_id", "orderDetail_order_id")
      .withColumnRenamed("order_price", "orderDetail_order_price")
      .withColumnRenamed("create_time", "orderDetail_create_time")
      .withColumnRenamed("table", "orderDetail_table")
      .withColumnRenamed("sku_id", "orderDetail_sku_id")
      .withColumnRenamed("img_url", "orderDetail_img_url")
      .withWatermark("orderDetail_create_time", "2 hours")


    val preferentialDStream = getDStream(bootStrapServers, "preferential_activities", "earliest", preferentialGroupid, "4800", sparkSession)
      .as[String].map(item => {
      JSON.parseObject[PreferentialActivities](item, classOf[PreferentialActivities])
    }).withColumn("create_time", to_timestamp(from_unixtime(col("create_time") / 1000)))
      .withColumnRenamed("id", "preferential_id")
      .withColumnRenamed("order_id", "preferential_order_id")
      .withColumnRenamed("create_time", "preferential_create_time")
      .withColumnRenamed("table", "preferential_table")
      .withWatermark("preferential_create_time", "2 hours")

    val couponDStream = getDStream(bootStrapServers, "coupon", "earliest", couponGroupid, "4800", sparkSession)
      .as[String].map(item => {
      JSON.parseObject[Coupon](item, classOf[Coupon])
    }).withColumn("create_time", to_timestamp(from_unixtime(col("create_time") / 1000)))
      .withColumnRenamed("id", "copon_id")
      .withColumnRenamed("order_id", "copon_order_id")
      .withColumnRenamed("create_time", "copon_create_time")
      .withColumnRenamed("table", "copon_table")
      .drop("sku_id").drop("order_detail_id")
      .withWatermark("copon_create_time", "2 hours")

    //事实表进行join
    val resultDStream = orderMainDStream.join(orderDetailDStream,
      orderMainDStream("id") === orderDetailDStream("orderDetail_order_id") &&
        orderMainDStream("orderMain_create_time") <= orderDetailDStream("orderDetail_create_time") &&
        (orderMainDStream("orderMain_create_time").+(expr("interval 1 hour")) >= (orderDetailDStream("orderDetail_create_time")))
      , "right")
      .join(preferentialDStream,
        orderMainDStream("id") === preferentialDStream("preferential_order_id") &&
          orderMainDStream("orderMain_create_time") <= preferentialDStream("preferential_create_time") &&
          orderMainDStream("orderMain_create_time").+(expr("interval 1 hour")) > preferentialDStream("preferential_create_time"),
        "left")
      .join(couponDStream,
        orderMainDStream("id") === couponDStream("copon_order_id") &&
          orderMainDStream("orderMain_create_time") <= couponDStream("copon_create_time") &&
          orderMainDStream("orderMain_create_time").+(expr("interval 1 hour")) > couponDStream("copon_create_time"),
        "left")
      .as[OrderDetails]
    //关联维度表
    resultDStream.writeStream
      .foreachBatch { (batchDF: Dataset[OrderDetails], batchId: Long) =>
        //批量查询list
        batchDF.cache()
        //申明三个HashMap,并且广播，用于存放维度表数据
        val brRuleDataMap = sparkContext.broadcast(new util.HashMap[String, String]())
        val brSkuRuleDataMap = sparkContext.broadcast(new util.HashMap[String, String])
        val brCoponDataMap = sparkContext.broadcast(new util.HashMap[String, String])
        //关联维度表数据
        getDimemsionData(batchDF, brRuleDataMap, brSkuRuleDataMap, brCoponDataMap)
        //获取宽表数据后,插入HBase
        putDimData(batchDF, brRuleDataMap, brSkuRuleDataMap, brCoponDataMap)
        batchDF.unpersist()
        brCoponDataMap.unpersist()
        brRuleDataMap.unpersist()
        brSkuRuleDataMap.unpersist()
      }
      .option("checkpointLocation", "hdfs://mycluster/structuredstreaming/checkpoint/dimorders")
      .start().awaitTermination()
  }

  def getDStream(bootStrapServers: String, subscribe: String, startingOffsets: String, groupid: String,
                 maxOffsetsPerTrigger: String, sparkSession: SparkSession) = {
    sparkSession.readStream.format("kafka")
      .option("kafka.bootstrap.servers", bootStrapServers)
      .option("subscribe", subscribe)
      .option("startingOffsets", startingOffsets)
      .option("kafka.group.id", groupid)
      .option("maxOffsetsPerTrigger", maxOffsetsPerTrigger)
      .load()
      .selectExpr("cast (value as string)")
  }

  case class OrderDetails(@BeanProperty id: String, @BeanProperty consignee: String, @BeanProperty consignee_tel: String, @BeanProperty total_amount: String, @BeanProperty order_status: String,
                          @BeanProperty user_id: String, @BeanProperty payment_way: String, @BeanProperty delivery_address: String, @BeanProperty order_comment: String, @BeanProperty out_trade_no: String,
                          @BeanProperty trade_body: String, @BeanProperty orderMain_create_time: String, @BeanProperty orderMain_operte_time: String, @BeanProperty expire_time: String,
                          @BeanProperty process_status: String, @BeanProperty tracking_no: String, @BeanProperty parent_order_id: String, @BeanProperty orderMain_img_url: String, @BeanProperty province_id: String,
                          @BeanProperty activity_reduce_amount: String, @BeanProperty coupon_reduce_amount: String, @BeanProperty original_total_amount: String,
                          @BeanProperty feight_fee: String, @BeanProperty feight_fee_reduce: String, @BeanProperty refundable_time: String, @BeanProperty orderMain_table: String, @BeanProperty orderDetail_id: String, @BeanProperty orderDetail_order_id: String,
                          @BeanProperty orderDetail_sku_id: String, @BeanProperty sku_name: String, @BeanProperty orderDetail_img_url: String, @BeanProperty orderDetail_order_price: String, @BeanProperty sku_num: String, @BeanProperty orderDetail_create_time: String,
                          @BeanProperty source_type: String, @BeanProperty source_id: String, @BeanProperty pay_amount: String, @BeanProperty split_total_amount: String, @BeanProperty split_activity_amount: String,
                          @BeanProperty split_coupon_amount: String, @BeanProperty orderDetail_table: String, @BeanProperty preferential_id: String, @BeanProperty preferential_order_id: String,
                          @BeanProperty order_detail_id: String, @BeanProperty activity_id: String, @BeanProperty activity_rule_id: String, @BeanProperty sku_id: String, @BeanProperty preferential_create_time: String,
                          @BeanProperty preferential_table: String, @BeanProperty copon_id: String, @BeanProperty copon_order_id: String, @BeanProperty coupon_id: String,
                          @BeanProperty coupon_use_id: String, @BeanProperty copon_create_time: String, @BeanProperty copon_table: String)

  def addActivityRuleGets(jsonObject: JSONObject, activityRuleGets: util.ArrayList[Get], activityRuleSkuGets: util.ArrayList[Get], couponInfoTableGets: util.ArrayList[Get]): Unit = {
    //根据id请求维度表数据
    val activity_id = jsonObject.getString("activity_id")
    val sku_id = jsonObject.getString("sku_id")
    val copon_id = jsonObject.getString("copon_id")
    val activityGet = new Get(Bytes.toBytes(Utils.generateHash(activity_id).substring(0, 5) + "_" + activity_id))
      .addColumn(Bytes.toBytes("info"), Bytes.toBytes("activity_type"))
      .addColumn(Bytes.toBytes("info"), Bytes.toBytes("condition_amount"))
      .addColumn(Bytes.toBytes("info"), Bytes.toBytes("benefit_amount"))
      .addColumn(Bytes.toBytes("info"), Bytes.toBytes("benefit_discount"))
      .addColumn(Bytes.toBytes("info"), Bytes.toBytes("benefit_level"))
    val skuGet = new Get(Bytes.toBytes(Utils.generateHash(sku_id).substring(0, 5) + "_" + sku_id))
      .addColumn(Bytes.toBytes("info"), Bytes.toBytes("sku_id"))
      .addColumn(Bytes.toBytes("info"), Bytes.toBytes("create_time"))
    val coponGet = new Get(Bytes.toBytes(Utils.generateHash(copon_id).substring(0, 5) + "_" + copon_id))
      .addColumn(Bytes.toBytes("info"), Bytes.toBytes("coupon_name"))
      .addColumn(Bytes.toBytes("info"), Bytes.toBytes("coupon_type"))
      .addColumn(Bytes.toBytes("info"), Bytes.toBytes("condition_amount"))
      .addColumn(Bytes.toBytes("info"), Bytes.toBytes("benefit_amount"))
      .addColumn(Bytes.toBytes("info"), Bytes.toBytes("benefit_discount"))
      .addColumn(Bytes.toBytes("info"), Bytes.toBytes("create_time"))
      .addColumn(Bytes.toBytes("info"), Bytes.toBytes("range_type"))
      .addColumn(Bytes.toBytes("info"), Bytes.toBytes("limit_num"))
      .addColumn(Bytes.toBytes("info"), Bytes.toBytes("taken_count"))
      .addColumn(Bytes.toBytes("info"), Bytes.toBytes("start_time"))
      .addColumn(Bytes.toBytes("info"), Bytes.toBytes("end_time"))
      .addColumn(Bytes.toBytes("info"), Bytes.toBytes("operate_time"))
      .addColumn(Bytes.toBytes("info"), Bytes.toBytes("expire_time"))
      .addColumn(Bytes.toBytes("info"), Bytes.toBytes("range_desc"))
    activityRuleGets.add(activityGet)
    activityRuleSkuGets.add(skuGet)
    couponInfoTableGets.add(coponGet)
  }

  /**
    * 关联维度表数据
    *
    * @param batchDF
    * @param brRuleDataMap
    * @param brSkuRuleDataMap
    * @param brCoponDataMap
    */
  def getDimemsionData(batchDF: Dataset[OrderDetails], brRuleDataMap: Broadcast[util.HashMap[String, String]],
                       brSkuRuleDataMap: Broadcast[util.HashMap[String, String]], brCoponDataMap: Broadcast[util.HashMap[String, String]]) = {
    batchDF.foreachPartition((partitions: Iterator[OrderDetails]) => {
      //批量查询Hbase 声明三个list用于存放get
      val activityRuleGets = new util.ArrayList[Get]()
      val activityRuleSkuGets = new util.ArrayList[Get]()
      val couponInfoTableGets = new util.ArrayList[Get]()
      import org.apache.hadoop.hbase.HBaseConfiguration
      import org.apache.hadoop.hbase.client.ConnectionFactory
      val hbaseConfig = HBaseConfiguration.create
      hbaseConfig.set("hbase.zookeeper.property.clientPort", "2181")
      hbaseConfig.set("hbase.zookeeper.quorum", "hadoop101,hadoop102,hadoop103")
      val connection = ConnectionFactory.createConnection(hbaseConfig)
      var activityRuleTable: Table = connection.getTable(TableName.valueOf("orders:dwd_activity_rule"))
      val activitySkuTable: Table = connection.getTable(TableName.valueOf("orders:dwd_activity_sku"))
      val couponInfoTable: Table = connection.getTable(TableName.valueOf("orders:dwd_coupon_info"))
      //循环分区数据，将get请求加入到list中
      partitions.foreach(item => {
        val jsonObject: JSONObject = JSON.parseObject(JSON.toJSONString(item, SerializerFeature.QuoteFieldNames))
        addActivityRuleGets(jsonObject, activityRuleGets, activityRuleSkuGets, couponInfoTableGets)
      })
      val ruleResult = activityRuleTable.get(activityRuleGets)
      val skuResult = activitySkuTable.get(activityRuleSkuGets)
      val coponResult = couponInfoTable.get(couponInfoTableGets)
      //批量查询出数据后，将结果数据放入到广播
      for (result <- ruleResult) {
        val cells = result.rawCells()
        for (cell <- cells) {
          brRuleDataMap.value.put(Bytes.toString(CellUtil.cloneRow(cell)) + "_rule_" + Bytes.toString(CellUtil.cloneQualifier(cell))
            , Bytes.toString(CellUtil.cloneValue(cell)))
        }
      }
      for (result <- skuResult) {
        val cells = result.rawCells()
        for (cell <- cells) {
          brSkuRuleDataMap.value.put(Bytes.toString(CellUtil.cloneRow(cell)) + "_sku_" + Bytes.toString(CellUtil.cloneQualifier(cell)), Bytes.toString(CellUtil.cloneValue(cell)))
        }
      }
      for (result <- coponResult) {
        val cells = result.rawCells()
        for (cell <- cells) {
          brCoponDataMap.value.put(Bytes.toString(CellUtil.cloneRow(cell)) + "_copon_" + Bytes.toString(CellUtil.cloneQualifier(cell)), Bytes.toString(CellUtil.cloneValue(cell)))
        }
      }
      activityRuleTable.close()
      activitySkuTable.close()
      couponInfoTable.close()
      connection.close()
    })
  }

  /**
    * 插入宽表数据
    *
    * @param batchDF
    * @param brRuleDataMap
    * @param brSkuRuleDataMap
    * @param brCoponDataMap
    */
  def putDimData(batchDF: Dataset[OrderDetails], brRuleDataMap: Broadcast[util.HashMap[String, String]],
                 brSkuRuleDataMap: Broadcast[util.HashMap[String, String]], brCoponDataMap: Broadcast[util.HashMap[String, String]]) = {
    batchDF.foreachPartition((partitions: Iterator[OrderDetails]) => {
      import org.apache.hadoop.hbase.HBaseConfiguration
      import org.apache.hadoop.hbase.client.ConnectionFactory
      val hbaseConfig = HBaseConfiguration.create
      hbaseConfig.set("hbase.zookeeper.property.clientPort", "2181")
      hbaseConfig.set("hbase.zookeeper.quorum", "hadoop101,hadoop102,hadoop103")
      val connection = ConnectionFactory.createConnection(hbaseConfig)
      val dimOrderDetailsTable = connection.getTable(TableName.valueOf("orders:dim_order_details"))
      partitions.foreach(item => {
        //与维度表进行join
        val putList = new util.ArrayList[Put]()
        partitions.foreach(item => {
          val jsonObject: JSONObject = JSON.parseObject(JSON.toJSONString(item, SerializerFeature.QuoteFieldNames))
          println(jsonObject.toString)
          val activity_id = jsonObject.getString("activity_id")
          val sku_id = jsonObject.getString("sku_id")
          val copon_id = jsonObject.getString("copon_id")
          //获取到维表的id后 分别去广播的hashmap里取维表数据
          val ruledataMap = brRuleDataMap.value
          val skudataMap = brSkuRuleDataMap.value
          val copondataMap = brCoponDataMap.value
          val activity_type = ruledataMap.getOrDefault(Utils.generateHash(activity_id).substring(0, 5) + "_" + activity_id + "_rule_activity_type", "")
          val condition_amount = ruledataMap.getOrDefault(Utils.generateHash(activity_id).substring(0, 5) + "_" + activity_id + "_rule_condition_amount", "")
          val benefit_amount = ruledataMap.getOrDefault(Utils.generateHash(activity_id).substring(0, 5) + "_" + activity_id + "_rule_benefit_amount", "")
          val benefit_discount = ruledataMap.getOrDefault(Utils.generateHash(activity_id).substring(0, 5) + "_" + activity_id + "_rule_benefit_discount", "")
          val benefit_level = ruledataMap.getOrDefault(Utils.generateHash(activity_id).substring(0, 5) + "_" + activity_id + "_rule_benefit_level", "")
          val sku_createtime = skudataMap.getOrDefault(Utils.generateHash(sku_id).substring(0, 5) + "_" + sku_id + "_sku_create_time", "")
          val coupon_name = copondataMap.getOrDefault(Utils.generateHash(copon_id).substring(0, 5) + "_" + copon_id + "_copon_coupon_name", "")
          val coupon_type = copondataMap.getOrDefault(Utils.generateHash(copon_id).substring(0, 5) + "_" + copon_id + "_copon_coupon_type", "")
          val coponCondition_amount = copondataMap.getOrDefault(Utils.generateHash(copon_id).substring(0, 5) + "_" + copon_id + "_copon_condition_amount", "")
          val coponCenefit_amount = copondataMap.getOrDefault(Utils.generateHash(copon_id).substring(0, 5) + "_" + copon_id + "_copon_benefit_amount", "")
          val coponBenefit_discount = copondataMap.getOrDefault(Utils.generateHash(copon_id).substring(0, 5) + "_" + copon_id + "_copon_benefit_discount", "")
          val coponCreate_time = copondataMap.getOrDefault(Utils.generateHash(copon_id).substring(0, 5) + "_" + copon_id + "_copon_create_time", "")
          val range_type = copondataMap.getOrDefault(Utils.generateHash(copon_id).substring(0, 5) + "_" + copon_id + "_copon_range_type", "")
          val limit_num = copondataMap.getOrDefault(Utils.generateHash(copon_id).substring(0, 5) + "_" + copon_id + "_copon_limit_num", "")
          val taken_count = copondataMap.getOrDefault(Utils.generateHash(copon_id).substring(0, 5) + "_" + copon_id + "_copon_taken_count", "")
          val start_time = copondataMap.getOrDefault(Utils.generateHash(copon_id).substring(0, 5) + "_" + copon_id + "_copon_start_time", "")
          val end_time = copondataMap.getOrDefault(Utils.generateHash(copon_id).substring(0, 5) + "_" + copon_id + "_copon_limit_num", "")
          val operate_time = copondataMap.getOrDefault(Utils.generateHash(copon_id).substring(0, 5) + "_" + copon_id + "_copon_operate_time", "")
          val expire_time = copondataMap.getOrDefault(Utils.generateHash(copon_id).substring(0, 5) + "_" + copon_id + "_copon_expire_time", "")
          val range_desc = copondataMap.getOrDefault(Utils.generateHash(copon_id).substring(0, 5) + "_" + copon_id + "_copon_range_desc", "")
          //取出维度表数据之后，将维度表数据添加到宽表jsonObject种
          jsonObject.put("activity_type", activity_type)
          jsonObject.put("condition_amount", condition_amount)
          jsonObject.put("benefit_amount", benefit_amount)
          jsonObject.put("benefit_discount", benefit_discount)
          jsonObject.put("benefit_level", benefit_level)
          jsonObject.put("sku_createtime", sku_createtime)
          jsonObject.put("coupon_name", coupon_name)
          jsonObject.put("coupon_type", coupon_type)
          jsonObject.put("coponCondition_amount", coponCondition_amount)
          jsonObject.put("coponBenefit_discount", coponBenefit_discount)
          jsonObject.put("coponCreate_time", coponCreate_time)
          jsonObject.put("range_type", range_type)
          jsonObject.put("limit_num", limit_num)
          jsonObject.put("taken_count", taken_count)
          jsonObject.put("start_time", start_time)
          jsonObject.put("end_time", end_time)
          jsonObject.put("operate_time", operate_time)
          jsonObject.put("expire_time", expire_time)
          jsonObject.put("range_desc", range_desc)
          //宽表主键, hash(订单主表ID).substring(0,6)+"_"+订单主表id+"_"+订单明细id
          val rowkey = Utils.generateHash(jsonObject.getString("id")).substring(0, 5) + "_" + jsonObject.getString("id") + "_" + jsonObject.getString("orderDetail_id")
          val put = new Put(Bytes.toBytes(rowkey))
          val keySet = jsonObject.keySet().toArray
          for (key <- keySet) {
            put.addColumn(Bytes.toBytes("info"), Bytes.toBytes(key.toString),
              Bytes.toBytes(jsonObject.getString(key.toString)))
          }
          putList.add(put)
          jsonObject.clear()
        })
        dimOrderDetailsTable.put(putList)
        putList.clear()
        dimOrderDetailsTable.close()
        connection.close()
      })
    })
  }
}
