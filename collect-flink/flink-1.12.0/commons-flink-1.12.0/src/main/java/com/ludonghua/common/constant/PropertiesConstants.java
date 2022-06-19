package com.ludonghua.common.constant;

public class PropertiesConstants {

    public static final String STREAM_PARALLELISM = "stream.parallelism";
    public static final String STREAM_SINK_PARALLELISM = "stream.sink.parallelism";

    public static final String STREAM_CHECKPOINT_ENABLE = "stream.checkpoint.enable";
    public static final String STREAM_CHECKPOINT_INTERVAL = "stream.checkpoint.interval";
    public static final String STREAM_CHECKPOINT_TIMEOUT = "stream.checkpoint.timeout";
    public static final String STREAM_CHECKPOINT_DIR = "stream.checkpoint.dir";

    // kafka
    public static final String KAFKA_BROKERS = "kafka.brokers";

    public static final String KAFKA_CONSUMER_TOPIC1 = "kafka.consumer.topic1";
    public static final String KAFKA_CONSUMER_GROUP_ID1 = "kafka.consumer.group.id1";
    public static final String KAFKA_CONSUMER_TOPIC2 = "kafka.consumer.topic2";
    public static final String KAFKA_CONSUMER_GROUP_ID2 = "kafka.consumer.group.id2";
    public static final String KAFKA_CONSUMER_TOPIC3 = "kafka.consumer.topic3";
    public static final String KAFKA_CONSUMER_GROUP_ID3 = "kafka.consumer.group.id3";

    public static final String KAFKA_PRODUCER_TOPIC1 = "kafka.producer.topic1";
    public static final String KAFKA_PRODUCER_TOPIC2 = "kafka.producer.topic2";
    public static final String KAFKA_PRODUCER_TOPIC3 = "kafka.producer.topic3";
    public static final String KAFKA_PRODUCER_TOPIC4 = "kafka.producer.topic4";
    public static final String KAFKA_PRODUCER_TOPIC5 = "kafka.producer.topic5";

    // es
    public static final String ELASTICSEARCH_HOSTS = "elasticsearch.hosts";
    public static final String ELASTICSEARCH_USERNAME = "elasticsearch.username";
    public static final String ELASTICSEARCH_PASSWORD = "elasticsearch.password";
    public static final String ELASTICSEARCH_INDEX1 = "elasticsearch.index1";
    public static final String ELASTICSEARCH_INDEX2 = "elasticsearch.index2";
    public static final String ELASTICSEARCH_INDEX3 = "elasticsearch.index3";
    public static final String ELASTICSEARCH_BULK_FLUSH_MAX_ACTIONS = "elasticsearch.bulk.flush.max.actions";




    public static final String ZHISHENG = "zhisheng";

    public static final String DEFAULT_KAFKA_BROKERS = "localhost:9092";
    public static final String KAFKA_ZOOKEEPER_CONNECT = "kafka.zookeeper.connect";
    public static final String DEFAULT_KAFKA_ZOOKEEPER_CONNECT = "localhost:2181";
    public static final String KAFKA_GROUP_ID = "kafka.group.id";
    public static final String DEFAULT_KAFKA_GROUP_ID = "zhisheng";
    public static final String METRICS_TOPIC = "metrics.topic";
    public static final String CONSUMER_FROM_TIME = "consumer.from.time";

    public static final String STREAM_DEFAULT_PARALLELISM = "stream.default.parallelism";

    public static final String STREAM_CHECKPOINT_TYPE = "stream.checkpoint.type";

    public static final String CHECKPOINT_MEMORY = "memory";
    public static final String CHECKPOINT_FS = "fs";
    public static final String CHECKPOINT_ROCKETSDB = "rocksdb";



    //mysql
    public static final String MYSQL_DATABASE = "mysql.database";
    public static final String MYSQL_HOST = "mysql.host";
    public static final String MYSQL_PASSWORD = "mysql.password";
    public static final String MYSQL_PORT = "mysql.port";
    public static final String MYSQL_USERNAME = "mysql.username";
}