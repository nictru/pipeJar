module pipeJar {
    requires org.apache.logging.log4j;
    requires org.json;

    exports org.exbio.pipejar.pipeline;
    exports org.exbio.pipejar.configs;
    exports org.exbio.pipejar.configs.ConfigTypes.FileTypes;
    exports org.exbio.pipejar.configs.ConfigTypes.UsageTypes;
    exports org.exbio.pipejar.configs.ConfigTypes.InputTypes;
    exports org.exbio.pipejar.configs.ConfigValidators;
    exports org.exbio.pipejar.util;
}