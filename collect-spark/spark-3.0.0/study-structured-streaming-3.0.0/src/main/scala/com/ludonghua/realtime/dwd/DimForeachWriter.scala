package com.ludonghua.realtime.dwd

import com.alibaba.fastjson.serializer.SerializerFeature
import com.alibaba.fastjson.{JSON, JSONObject}
import com.ludonghua.realtime.dwd.DimStructuredStream.OrderDetails
import com.ludonghua.realtime.utils.Utils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.{CellUtil, HBaseConfiguration, TableName}
import org.apache.spark.sql.ForeachWriter

import java.util.concurrent.{ExecutorService, TimeUnit}

object DimForeachWriter extends ForeachWriter[OrderDetails] {
  //  var executorService: ExecutorService = _
  var hbaseConfig: Configuration = _
  var connection: Connection = _
  var activityRuleTable: Table = _
  var activitySkuTable: Table = _
  var couponInfoTable: Table = _
  var dimOrderDetailsTable: Table = _


  override def open(partitionId: Long, epochId: Long): Boolean = {
    //    executorService = Executors.newFixedThreadPool(12)
    hbaseConfig = HBaseConfiguration.create()
    hbaseConfig.set("hbase.zookeeper.property.clientPort", "2181")
    hbaseConfig.set("hbase.zookeeper.quorum", "hadoop101,hadoop102,hadoop103")
    connection = ConnectionFactory.createConnection(hbaseConfig)
    activityRuleTable = connection.getTable(TableName.valueOf("orders:dwd_activity_rule"))
    activitySkuTable = connection.getTable(TableName.valueOf("orders:dwd_activity_sku"))
    couponInfoTable = connection.getTable(TableName.valueOf("orders:dwd_coupon_info"))
    dimOrderDetailsTable = connection.getTable(TableName.valueOf("orders:dim_order_details"))
    true
  }

  override def process(model: OrderDetails): Unit = {
    //关联维度表 获取到最终宽表
    val result = getDimensionData(model, activityRuleTable, activitySkuTable, couponInfoTable)
    //将宽表写入Hbase
    putOrderDetails(result, dimOrderDetailsTable)
  }

  override def close(errorOrNull: Throwable): Unit = {
    dimOrderDetailsTable.close()
    couponInfoTable.close()
    activitySkuTable.close()
    activityRuleTable.close()
    connection.close()
    //    gracefulShutdown(10, TimeUnit.SECONDS, executorService)
  }

  /**
    * join维度表
    *
    * @param data
    * @param activityRuleTable
    * @param activitySkuTable
    * @param couponInfoTable
    */
  def getDimensionData(data: OrderDetails, activityRuleTable: Table, activitySkuTable: Table, couponInfoTable: Table) = {
    //根据id请求维度表数据
    val jsonObject: JSONObject = JSON.parseObject(JSON.toJSONString(data, SerializerFeature.QuoteFieldNames))
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
    val activityResult = activityRuleTable.get(activityGet)
    val skuResult = activitySkuTable.get(skuGet)
    val coponResult = couponInfoTable.get(skuGet)
    for (cell <- activityResult.rawCells()) {
      val colName = Bytes.toString(CellUtil.cloneQualifier(cell))
      val value = Bytes.toString(CellUtil.cloneValue(cell))
      jsonObject.put(colName, value)
    }
    for (cell <- skuResult.rawCells()) {
      val colName = Bytes.toString(CellUtil.cloneQualifier(cell))
      val value = Bytes.toString(CellUtil.cloneValue(cell))
      colName match {
        case "sku_id" => jsonObject.put("wei_sku_id", value)
        case "create_time" => jsonObject.put("sku_createtime", value)
        case _ => jsonObject.put(colName, value)
      }
    }
    for (cell <- coponResult.rawCells()) {
      val colName = Bytes.toString(CellUtil.cloneQualifier(cell))
      val value = Bytes.toString(CellUtil.cloneValue(cell))
      colName match {
        case "condition_amount" => jsonObject.put("coupon_condition_amount", value)
        case "benefit_amount" => jsonObject.put("copon_benefit_amount", value)
        case "benefit_discount" => jsonObject.put("copon_benefit_discount", value)
        case "create_time" => jsonObject.put("copon_create_time", value)
        case "start_time" => jsonObject.put("copon_start_time", value)
        case "end_time" => jsonObject.put("copon_end_time", value)
        case "operate_time" => jsonObject.put("copon_operate_time", value)
        case "expire_time" => jsonObject.put("copon_expire_time", value)
        case _ => jsonObject.put(colName, value)
      }
    }
    jsonObject
  }

  /**
    * 将宽表数据写入HBase
    *
    * @param data
    * @param dimOrderDetails
    */
  def putOrderDetails(data: JSONObject, dimOrderDetails: Table) = {
    val orderid = data.getString("id")
    val order_detail_id = data.getString("order_detail_id")
    val rowkey = Utils.generateHash(orderid).substring(0, 5) + "_" + orderid + "_" + order_detail_id
    val put = new Put(Bytes.toBytes(rowkey))
    val keySet = data.keySet().toArray()
    for (key <- keySet) {
      put.addColumn(Bytes.toBytes("info"), Bytes.toBytes(key.toString), Bytes.toBytes(data.getString(key.toString)))
    }
    dimOrderDetails.put(put)
  }

  /**
    * 线程池优雅关闭
    *
    * @param timeout
    * @param unit
    * @param executorServices
    */
  def gracefulShutdown(timeout: Long, unit: TimeUnit, executorServices: ExecutorService*): Unit = {
    for (executorService <- executorServices) {
      executorService.shutdown()
    }
    var wasInterrupted = false
    val endTime = unit.toMillis(timeout) + System.currentTimeMillis
    var timeLeft = unit.toMillis(timeout)
    var hasTimeLeft = timeLeft > 0L
    for (executorService <- executorServices) {
      if (wasInterrupted || !hasTimeLeft) executorService.shutdownNow
      else {
        try
            if (!executorService.awaitTermination(timeLeft, TimeUnit.MILLISECONDS)) {
              executorService.shutdownNow
            }
        catch {
          case e: InterruptedException =>
            executorService.shutdownNow
            wasInterrupted = true
            Thread.currentThread.interrupt()
        }
        timeLeft = endTime - System.currentTimeMillis
        hasTimeLeft = timeLeft > 0L
      }
    }
  }

}
