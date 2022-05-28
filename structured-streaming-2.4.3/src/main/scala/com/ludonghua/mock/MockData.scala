package com.ludonghua.mock

import com.ludonghua.mock.bean.CityInfo
import com.ludonghua.mock.utils.{RandomNumUtil, RandomOptions}

import java.util.Properties
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import scala.collection.mutable.ArrayBuffer

/**
 * Author Luis
 * DATE 2022-05-28 20:07
 *
 * 生成实时的模拟数据
 */
object MockData {
  /*
      数据格式:
      timestamp area city userId adId
      某个时间点 某个地区 某个城市 某个用户 某个广告

      */
  def mockRealTimeData(): ArrayBuffer[String] = {
    // 存储模拟的实时数据
    val array: ArrayBuffer[String] = ArrayBuffer[String]()
    // 城市信息
    val randomOpts: RandomOptions[CityInfo] = RandomOptions(
      (CityInfo(1, "北京", "华北"), 30),
      (CityInfo(2, "上海", "华东"), 30),
      (CityInfo(3, "广州", "华南"), 10),
      (CityInfo(4, "深圳", "华南"), 20),
      (CityInfo(4, "杭州", "华中"), 10))
    (1 to 50).foreach {
      _ => {
        val timestamp: Long = System.currentTimeMillis()
        val cityInfo: CityInfo = randomOpts.getRandomOption()
        val area: String = cityInfo.area
        val city: String = cityInfo.city_name
        val userId: Int = RandomNumUtil.randomInt(100, 105)
        val adId: Int = RandomNumUtil.randomInt(1, 5)
        array += s"$timestamp,$area,$city,$userId,$adId"
        Thread.sleep(10)
      }
    }
    array
  }

  def createKafkaProducer: KafkaProducer[String, String] = {
    val props: Properties = new Properties
    // Kafka服务端的主机名和端口号
    props.put("bootstrap.servers", "hadoop102:9092,hadoop103:9092,hadoop104:9092")
    // key序列化
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    // value序列化
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    new KafkaProducer[String, String](props)
  }

  def main(args: Array[String]): Unit = {
    val topic = "ads_log"
    val producer: KafkaProducer[String, String] = createKafkaProducer
    while (true) {
      mockRealTimeData().foreach {
        msg => {
          producer.send(new ProducerRecord(topic, msg))
          Thread.sleep(100)
        }
      }
      Thread.sleep(1000)
    }
  }
}
