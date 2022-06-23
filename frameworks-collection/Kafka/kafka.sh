#!/bin/bash

kafka_home=/opt/module/kafka

case $1 in
    start)
        for host in hadoop102 hadoop103 hadoop104;
        do
            echo "========== $host ========="
            ssh $host "source /etc/profile; nohup $kafka_home/bin/kafka-server-start.sh $kafka_home/config/server.properties >$kafka_home/kafka.log 2>&1 & "
        done
       ;;
    stop)
        for host in hadoop102 hadoop103 hadoop104;
        do
            echo "========== $host ========="
            ssh $host "source /etc/profile; nohup $kafka_home/bin/kafka-server-stop.sh "
        done
       ;;
    *)
        echo "你启动的姿势不对"
        echo "  start   启动kafka集群"
        echo "  stop    停止kafka集群"
    ;;
esac
