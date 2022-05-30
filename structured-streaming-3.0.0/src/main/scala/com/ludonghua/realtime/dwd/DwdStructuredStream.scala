package com.ludonghua.realtime.dwd

import com.alibaba.fastjson.{JSON, JSONObject}
import com.ludonghua.realtime.utils.Utils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.client.{Connection, ConnectionFactory, Put, Table}
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.{HBaseConfiguration, TableName}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.spark.SparkConf
import org.apache.spark.sql.{ForeachWriter, SparkSession}

import java.util.Properties


object DwdStructuredStream {
  val groupid = "orders_dwd_stream_groupid"

  def main(args: Array[String]): Unit = {
    val sparkConf = new SparkConf().setAppName("dwdOrderStream")
      .set("spark.sql.shuffle.partitions", "12")
    val sparkSession = SparkSession.builder().config(sparkConf).getOrCreate()
    import sparkSession.implicits._
    val dStream = sparkSession.readStream.format("kafka")
      .option("kafka.bootstrap.servers", "hadoop101:9092,hadoop102:9092,hadoop103:9092")
      .option("subscribe", "order_all")
      .option("startingOffsets", "earliest")
      .option("kafka.group.id", groupid)
      .option("maxOffsetsPerTrigger", "4800")
      .load()
      .selectExpr("cast(value as string)").as[String]


    dStream.writeStream.foreach(new ForeachWriter[String] {
      var hbaseConfig: Configuration = _
      var connection: Connection = _
      var activityRuleTable: Table = _
      var activitySkuTable: Table = _
      var couponInfoTable: Table = _
      val props = new Properties
      props.put("bootstrap.servers", "hadoop101:9092,hadoop102:9092,hadoop103:9092")
      props.put("acks", "-1")
      props.put("buffer.memory", "5000000")
      props.put("max.block.ms", "300000")
      props.put("compression.type", "snappy")
      props.put("linger.ms", "50")
      props.put("retries", Integer.MAX_VALUE.toString)
      props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
      props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
      var producer: KafkaProducer[String, String] = _

      override def open(partitionId: Long, epochId: Long): Boolean = {
        hbaseConfig = HBaseConfiguration.create()
        hbaseConfig.set("hbase.zookeeper.property.clientPort", "2181")
        hbaseConfig.set("hbase.zookeeper.quorum", "hadoop101,hadoop102,hadoop103")
        connection = ConnectionFactory.createConnection(hbaseConfig)
        activityRuleTable = connection.getTable(TableName.valueOf("orders:dwd_activity_rule"))
        activitySkuTable = connection.getTable(TableName.valueOf("orders:dwd_activity_sku"))
        couponInfoTable = connection.getTable(TableName.valueOf("orders:dwd_coupon_info"))
        producer = new KafkaProducer[String, String](props)
        true
      }

      override def process(value: String): Unit = {
        val jsonObject = JSON.parseObject(value)
        val table = jsonObject.getString("table")
        table match {
          case "activity_rule" => {
            upserDwdData(jsonObject, activityRuleTable)
          }
          case "activity_sku" => {
            upserDwdData(jsonObject, activitySkuTable)
          }
          case "coupon_info" => {
            upserDwdData(jsonObject, couponInfoTable)
          }
          case "OrderMain" => {
            producer.send(new ProducerRecord[String, String]("order_main", jsonObject.toJSONString))
          }
          case "OrderDeatail" => {
            producer.send(new ProducerRecord[String, String]("order_details", jsonObject.toJSONString))
          }
          case "PreferentialActivities" => {
            producer.send(new ProducerRecord[String, String]("preferential_activities", jsonObject.toJSONString))
          }
          case "Coupon" => {
            producer.send(new ProducerRecord[String, String]("coupon", jsonObject.toJSONString))
          }
          case _ => ""
        }
      }

      override def close(errorOrNull: Throwable): Unit = {
        producer.flush()
        producer.close()
        activityRuleTable.close()
        activitySkuTable.close()
        couponInfoTable.close()
      }
    }).option("checkpointLocation", "hdfs://mycluster/structuredstreaming/checkpoint/" + groupid).start().awaitTermination()


    def upserDwdData(jsonObject: JSONObject, hbaseTable: Table) = {
      val ts = jsonObject.getLong("ts")
      val data = jsonObject.getJSONObject("data")
      val id = data.getIntValue("id")
      val rowkey = Utils.generateHash(String.valueOf(id)).substring(0, 5) + "_" + id
      val put = new Put(Bytes.toBytes(rowkey))
      val keySet = data.keySet().toArray()
      put.addColumn(Bytes.toBytes("info"), Bytes.toBytes("ts"), Bytes.toBytes(ts))
      for (key <- keySet) {
        put.addColumn(Bytes.toBytes("info"), Bytes.toBytes(key.toString), Bytes.toBytes(data.getString(key.toString)))
      }
      hbaseTable.put(put)
    }

  }
}
