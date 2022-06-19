package com.ludonghua.use.sink;

import com.ludonghua.use.bean.WaterSensor;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

/**
 * Author Luis
 * DATE 2022-06-06 20:02
 */
public class SinkMySink {
    public static void main(String[] args) throws Exception {
        // 1、获取执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        // 2、读取端口数据并转换为JavaBean
        SingleOutputStreamOperator<WaterSensor> waterSensorDS = env.socketTextStream("hadoop102", 9999)
//        SingleOutputStreamOperator<WaterSensor> waterSensorDS = env.readTextFile("input/sensor.txt")
                .map(new MapFunction<String, WaterSensor>() {
                    @Override
                    public WaterSensor map(String value) throws Exception {
                        String[] split = value.split(",");
                        return new WaterSensor(split[0],
                                Long.parseLong(split[1]),
                                Integer.parseInt(split[2]));
                    }
                });

        // 3、将数据写入Mysql
        waterSensorDS.addSink(new MySink());


        // 4、执行
        env.execute();
    }

    public static class MySink extends RichSinkFunction<WaterSensor> {

        // 声明连接
        private Connection connection;
        private PreparedStatement preparedStatement;

        @Override
        public void open(Configuration parameters) throws Exception {
            connection = DriverManager.getConnection("jdbc:mysql://hadoop102:3306/test?useSSL=false", "root", "123123");
            preparedStatement = connection.prepareStatement("insert into sensor values(?, ?, ?) on duplicate key update ts = ?, vc = ?");
        }

        @Override
        public void invoke(WaterSensor value, Context context) throws Exception {

            // 给占位符赋值
            preparedStatement.setString(1, value.getId());
            preparedStatement.setLong(2, value.getTs());
            preparedStatement.setInt(3, value.getVc());
            preparedStatement.setLong(4, value.getTs());
            preparedStatement.setInt(5, value.getVc());

            // 执行操作
            preparedStatement.execute();
        }

        @Override
        public void close() throws Exception {
            preparedStatement.close();
            connection.close();
        }
    }
}
