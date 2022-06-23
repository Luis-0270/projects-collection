package com.ludonghua.producer

import java.text.DecimalFormat
import java.util.{Properties, Random}

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.serializer.SerializerFeature
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import scala.beans.BeanProperty

/**
  * 订单主表
  *
  * @param id
  * @param consignee
  * @param consignee_tel
  * @param total_amount
  * @param order_status
  * @param user_id
  * @param payment_way
  * @param delivery_address
  * @param order_comment
  * @param out_trade_no
  * @param trade_body
  * @param create_time
  * @param operate_time
  * @param expire_time
  * @param process_status
  * @param tracking_no
  * @param parent_order_id
  * @param img_url
  * @param province_id
  * @param activity_reduce_amount
  * @param coupon_reduce_amount
  * @param original_total_amount
  * @param feight_fee
  * @param feight_fee_reduce
  * @param refundable_time
  */
case class OrderMain(@BeanProperty id: Long, @BeanProperty consignee: String, @BeanProperty consignee_tel: String, @BeanProperty total_amount: String, @BeanProperty order_status: String,
                     @BeanProperty user_id: Long, @BeanProperty payment_way: String, @BeanProperty delivery_address: String, @BeanProperty order_comment: String, @BeanProperty out_trade_no: Long,
                     @BeanProperty trade_body: String, @BeanProperty create_time: Long, @BeanProperty operate_time: String, @BeanProperty expire_time: Long, @BeanProperty process_status: String,
                     @BeanProperty tracking_no: String, @BeanProperty parent_order_id: String, @BeanProperty img_url: String, @BeanProperty province_id: Int, @BeanProperty activity_reduce_amount: String,
                     @BeanProperty coupon_reduce_amount: String, @BeanProperty original_total_amount: String, @BeanProperty feight_fee: String, @BeanProperty feight_fee_reduce: String,
                     @BeanProperty refundable_time: String, @BeanProperty table: String)

/**
  * 订单明细表
  *
  * @param id
  * @param order_id
  * @param sku_id
  * @param sku_name
  * @param img_url
  * @param order_price
  * @param sku_num
  * @param create_time
  * @param source_type
  * @param source_id
  * @param pay_amount
  * @param split_total_amount
  * @param split_activity_amount
  * @param split_coupon_amount
  */
case class OrderDeatails(@BeanProperty id: Long, @BeanProperty order_id: Long, @BeanProperty sku_id: Int, @BeanProperty sku_name: String, @BeanProperty img_url: String, @BeanProperty order_price: String,
                         @BeanProperty sku_num: Int, @BeanProperty create_time: Long, @BeanProperty source_type: String, @BeanProperty source_id: Int, @BeanProperty pay_amount: String,
                         @BeanProperty split_total_amount: String, @BeanProperty split_activity_amount: String, @BeanProperty split_coupon_amount: String, @BeanProperty table: String)

/**
  * 优惠活动表
  *
  * @param id
  * @param order_id
  * @param order_detail_id
  * @param activity_id
  * @param activity_rule_id
  * @param sku_id
  * @param create_time
  */
case class PreferentialActivities(@BeanProperty id: Long, @BeanProperty order_id: Long, @BeanProperty order_detail_id: Long, @BeanProperty activity_id: Int,
                                  @BeanProperty activity_rule_id: Int, @BeanProperty sku_id: Int, @BeanProperty create_time: Long, @BeanProperty table: String)

/**
  * 优惠卷
  *
  * @param id
  * @param order_id
  * @param order_detail_id
  * @param coupon_id
  * @param coupon_use_id
  * @param sku_id
  * @param create_time
  */
case class Coupon(@BeanProperty id: Long, @BeanProperty order_id: Long, @BeanProperty order_detail_id: Long,
                  @BeanProperty coupon_id: Int, @BeanProperty coupon_use_id: Int, @BeanProperty sku_id: Int,
                  @BeanProperty create_time: Long, @BeanProperty table: String)


/**
  * 活动规则维度表
  *
  * @param database
  * @param table
  * @param `type`
  * @param data
  * @param ts
  */
case class ActivityRulesDim(@BeanProperty database: String, @BeanProperty table: String, @BeanProperty `type`: String,
                            @BeanProperty data: RuleData, @BeanProperty ts: Long)

case class RuleData(@BeanProperty id: Int, @BeanProperty activity_id: Int, @BeanProperty activity_type: Int,
                    @BeanProperty condition_amount: Double, @BeanProperty condition_num: Int, @BeanProperty benefit_amount: Double,
                    @BeanProperty benefit_discount: Int, @BeanProperty benefit_level: Int)

/**
  * 活动范围维度表
  *
  * @param database
  * @param table
  * @param `type`
  * @param data
  * @param ts
  */
case class ActivityRangeDim(@BeanProperty database: String, @BeanProperty table: String, @BeanProperty `type`: String,
                            @BeanProperty data: RangeData, @BeanProperty ts: Long)

case class RangeData(@BeanProperty id: Int, @BeanProperty sku_id: Int, @BeanProperty create_time: Long)

/**
  * 购物券维度表
  *
  * @param database
  * @param table
  * @param `type`
  * @param data
  * @param ts
  */
case class ShoppingVoucherDim(@BeanProperty database: String, @BeanProperty table: String, `type`: String, @BeanProperty data: VoucherData, @BeanProperty ts: Long)

case class VoucherData(@BeanProperty id: Int, @BeanProperty coupon_name: String, @BeanProperty coupon_type: Int, @BeanProperty condition_amount: Double,
                       @BeanProperty condition_num: Int, @BeanProperty activity_id: Int, @BeanProperty benefit_amount: Double,
                       @BeanProperty benefit_discount: Int, @BeanProperty create_time: Long, @BeanProperty range_type: Long,
                       @BeanProperty limit_num: Int, @BeanProperty taken_count: Int, @BeanProperty start_time: Long, @BeanProperty end_time: Long)


object OdsDataProducer {

  def main(args: Array[String]): Unit = {
    val props = new Properties
    props.put("bootstrap.servers", "hadoop102:9092,hadoop103:9092,hadoop104:9092")
    props.put("acks", "-1")
    props.put("buffer.memory", "5000000")
    props.put("max.block.ms", "300000")
    props.put("compression.type", "snappy")
    props.put("linger.ms", "50")
    props.put("retries", Integer.MAX_VALUE.toString)
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

    try {
      val producer = new KafkaProducer[String, String](props)

      /**
        * ---------------------------------------------维度表数据----------------------------------------------
        * 添加活动规则信息表数据
        */
      for (i <- 0 until 1000) {
        val model = generateActivityRulesDim(i)
        producer.send(new ProducerRecord[String, String]("order_all",
          JSON.toJSONString(model, SerializerFeature.QuoteFieldNames)))
      }
      producer.flush()

      /**
        * 添加活动范围数据
        */
      for (i <- 0 until 800000) {
        val model = generateActivityRangeDim(i)
        producer.send(new ProducerRecord[String, String]("order_all",
          JSON.toJSONString(model, SerializerFeature.QuoteFieldNames)))
      }
      producer.flush()

      /**
        * 添加购物券数
        */
      for (i <- 0 until 1000) {
        val model = generateShoppingVoucherDim(i)
        producer.send(new ProducerRecord[String, String]("order_all",
          JSON.toJSONString(model, SerializerFeature.QuoteFieldNames)))
      }
      producer.flush()


      /** -------------------------------------------事实表数据---------------------------------------
        * 添加商品信息表
        */
      for (i <- 0 until 3000000) {
        val model = generateOrderMainLog(i)
        producer.send(new ProducerRecord[String, String]("order_all",
          JSON.toJSONString(model, SerializerFeature.QuoteFieldNames)))
      }
      producer.flush()

      /**
        * 添加商品明细表
        */
      for (i <- 0 until 5000000) {
        val model = generateOrderDetails(i)
        producer.send(new ProducerRecord[String, String]("order_all",
          JSON.toJSONString(model, SerializerFeature.QuoteFieldNames)))
      }
      producer.flush()

      /**
        * 添加优惠活动数据
        */
      for (i <- 0 until 2000000) {
        val model = generateOrderDetailActivity(i)
        producer.send(new ProducerRecord[String, String]("order_all",
          JSON.toJSONString(model, SerializerFeature.QuoteFieldNames)))
      }
      producer.flush()

      /**
        * 添加优惠卷事实表数据
        */
      for (i <- 0 until 3000000) {
        val model = generateOrderDetailCoupon(i)
        producer.send(new ProducerRecord[String, String]("order_all",
          JSON.toJSONString(model, SerializerFeature.QuoteFieldNames)))
      }
      producer.flush()
    } catch {
      case e: Exception => println(e.getCause.getMessage)
    }
  }

  def generateOrderMainLog(id: Int): OrderMain = {
    val df = new DecimalFormat("0.00")
    val random = new Random()
    val consignee = "用户" + id;
    val consignee_tel = "13315310287"
    val toal_amount = df.format(random.nextDouble() * 1000)
    val order_status = "1001"
    val user_id = random.nextInt(10000)
    val payment_way = null
    val delivery_address = "第7大街第" + id + "号楼4单元629门"
    val order_comment = "描述" + id
    val out_trade_no = random.nextLong()
    val trade_body = "小米（MI） 小米路由器4 双千兆路由器 无线家用穿墙1200M高速双频wifi 千兆版 千兆端口光纤适用等3件商品"
    val create_time = System.currentTimeMillis()
    val operate_time = null;
    val expire_time = System.currentTimeMillis()
    val process_status = null
    val tracking_no = null;
    val parent_order_id = null
    val img_url = "http://img.gmall.com/169344.jpg"
    val province_id = random.nextInt(100)
    val activity_reduce_amount = "0.00"
    val coupon_reduce_amount = "0.00"
    val original_total_amount = "10"
    val feight_fee = "10"
    val feight_fee_reduce = null
    val refundable_time = null
    new OrderMain(id, consignee, consignee_tel, toal_amount, order_status, user_id, payment_way, delivery_address, order_comment
      , out_trade_no, tracking_no, create_time, operate_time, expire_time, process_status, tracking_no, parent_order_id, img_url, province_id,
      activity_reduce_amount, coupon_reduce_amount, original_total_amount, feight_fee, feight_fee_reduce, refundable_time, "OrderMain")
  }

  /**
    * 生成800万条明细
    *
    * @param id
    * @return
    */
  def generateOrderDetails(id: Int): OrderDeatails = {
    val df = new DecimalFormat("0.00")
    val random = new Random()
    val order_id = random.nextInt(3000000)
    val sku_id = random.nextInt(800000) //80万条sku
    val sku_name = "Dior迪奥口红唇膏送女友老婆礼物生日礼物 烈艳蓝金999+888两支装礼盒"
    val img_url = "http://kAXllAQEzJWHwiExxVmyJIABfXyzbKwedeofMqwh"
    val order_price = "496.00"
    val sku_num = 3
    val createtime = System.currentTimeMillis()
    val source_type = "00000"
    val source_id = 0
    val pay_amount = null
    val split_total_amount = null
    val split_activity_amount = null
    val split_coupon_amount = "100.00"
    new OrderDeatails(id, order_id, sku_id, sku_name, img_url, order_price, sku_num, createtime, source_type, source_id, pay_amount,
      split_total_amount, split_activity_amount, split_coupon_amount, "OrderDeatail")
  }

  /**
    * 生成200万条优惠活动数据
    *
    * @param id
    */
  def generateOrderDetailActivity(id: Int) = {
    val random = new Random()
    val order_id = random.nextInt(3000000)
    val order_detail_id = 0
    val activity_id = random.nextInt(1000) // 1000条活动
    val actiity_rule_id = random.nextInt(1000) //1000条活动规则
    val sku_id = 0
    val create_time = System.currentTimeMillis()
    new PreferentialActivities(id, order_id, order_detail_id, actiity_rule_id, actiity_rule_id, sku_id, create_time, "PreferentialActivities")
  }

  /**
    * 生成300万订单优惠卷
    *
    * @param id
    */
  def generateOrderDetailCoupon(id: Int) = {
    val random = new Random()
    val order_id = random.nextInt(3000000)
    val order_detail_id = 12756
    val coupon_id = random.nextInt(1000) //1000条优惠卷
    val coupon_use_id = 0
    val sku_id = 0
    val createtime = System.currentTimeMillis()
    new Coupon(id, order_id, order_detail_id, coupon_id, coupon_use_id, sku_id, createtime, "Coupon")
  }

  /**
    * 生成活动规则
    *
    * @param id
    * @return
    */
  def generateActivityRulesDim(id: Int): ActivityRulesDim = {
    val database = "gmall"
    val table = "activity_rule"
    val `type` = "bootstrap-insert"
    val ts = System.currentTimeMillis()
    val activity_id = 1
    val activity_type = 3101
    val condition_amount = 10000.00
    val condition_num = 0
    val benefit_amount = 500.00
    val benefit_discount = 0
    val benefit_level = 1
    new ActivityRulesDim(database, table, `type`, new RuleData(id, activity_id, activity_type, condition_amount, condition_num,
      benefit_amount, benefit_discount, benefit_level), ts)
  }

  /**
    * 生成活动规则
    *
    * @param id
    * @return
    */
  def generateActivityRangeDim(id: Int) = {
    val database = "gmall"
    val table = "activity_sku"
    val `type` = "bootstrap-insert"
    val ts = System.currentTimeMillis()
    val activity_id = 1
    val sku_id = id
    val create_time = System.currentTimeMillis()
    new ActivityRangeDim(database, table, `type`, new RangeData(id, sku_id, create_time), ts)
  }

  def generateShoppingVoucherDim(id: Int) = {
    val database = "gmall"
    val table = "coupon_info"
    val `type` = "bootstrap-insert"
    val ts = System.currentTimeMillis()
    val coupon_name = "口红品类卷" + id
    val coupon_type = 3201
    val condition_amount = 99.00
    val condition_num = 0
    val activity_id = 0
    val benefit_amount = 30.00
    val benefit_discount = 0
    val create_time = System.currentTimeMillis()
    val range_type = 3301
    val limit_num = 100
    val taken_count = 0
    val start_time = create_time
    val end_time = create_time
    new ShoppingVoucherDim(database, table, `type`, new VoucherData(id, coupon_name, coupon_type, condition_amount, condition_num, activity_id,
      benefit_amount, benefit_discount, create_time, range_type, limit_num, taken_count, start_time, end_time), ts)
  }
}
