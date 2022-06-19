package com.ludonghua.common.utils;

import com.ludonghua.common.constant.PropertiesConstants;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.runtime.state.filesystem.FsStateBackend;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class ExecutionEnvUtil {

    public static ParameterTool createParameterTool(final String[] args, String propertiesName) throws Exception {

        if (args.length == 1) {
            // 获取配置参数 --configPath online-streaming/conf/config_test.properties
            // idata2 不支持参数 -- 或者 - , 拼接了下 String[] args
            String arg = args[0];
            String[] argsArray = {"--configPath", arg};
            String configPath = ParameterTool.fromArgs(argsArray).get("configPath");

            return ParameterTool
                    .fromPropertiesFile(configPath)
                    .mergeWith(ParameterTool.fromSystemProperties());
        } else {
            return ParameterTool
                    .fromPropertiesFile(ExecutionEnvUtil.class.getResourceAsStream(propertiesName))
                    .mergeWith(ParameterTool.fromSystemProperties());
        }
    }

    public static StreamExecutionEnvironment createEnv(ParameterTool parameterTool, Boolean isLocal) {

        StreamExecutionEnvironment env;

        if (isLocal) {
            Configuration config = new Configuration();
            config.setString(RestOptions.BIND_PORT, "8081");
            env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(config);
        } else {
            env = StreamExecutionEnvironment.getExecutionEnvironment();
        }

        env.setParallelism(parameterTool.getInt(PropertiesConstants.STREAM_PARALLELISM, 1));
        if (parameterTool.getBoolean(PropertiesConstants.STREAM_CHECKPOINT_ENABLE, false)) {
            env.enableCheckpointing(parameterTool.getLong(PropertiesConstants.STREAM_CHECKPOINT_INTERVAL, 300000L));
            env.getCheckpointConfig().setCheckpointTimeout(parameterTool.getLong(PropertiesConstants.STREAM_CHECKPOINT_TIMEOUT,600000L));
            env.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);
            env.getCheckpointConfig().enableExternalizedCheckpoints(CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);
            env.getConfig().setRestartStrategy(RestartStrategies.fixedDelayRestart(3, 60000L));
            env.setStateBackend(new FsStateBackend(parameterTool.get(PropertiesConstants.STREAM_CHECKPOINT_DIR)));
        }
        env.getConfig().setGlobalJobParameters(parameterTool);
        return env;
    }
}