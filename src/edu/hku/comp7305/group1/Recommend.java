package edu.hku.comp7305.group1;


import org.apache.hadoop.mapred.JobConf;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Recommend {

    public static final String HDFS = "hdfs://student3-x1:9000";
    public static final Pattern DELIMITER = Pattern.compile("[\t,]");

    public static final String BASE_PATH = HDFS + "/user/hdfs/recommend";

//    public static final String STEP_1_INPUT_PATH = BASE_PATH;
    public static final String STEP_1_OUTPUT_PATH = BASE_PATH + "/step1";

    public static final String STEP_2_INPUT_PATH = STEP_1_OUTPUT_PATH;
    public static final String STEP_2_OUTPUT_PATH = BASE_PATH + "/step2";

    public static final String STEP_3_1_INPUT_PATH = STEP_1_OUTPUT_PATH;
    public static final String STEP_3_1_OUTPUT_PATH = BASE_PATH + "/step3_1";

    public static final String STEP_3_2_INPUT_PATH = STEP_2_OUTPUT_PATH;
    public static final String STEP_3_2_OUTPUT_PATH = BASE_PATH + "/step3_2";

    public static final String STEP_5_1_INPUT_PATH = STEP_3_1_OUTPUT_PATH;
    public static final String STEP_5_2_INPUT_PATH = STEP_3_2_OUTPUT_PATH;
    public static final String STEP_5_OUTPUT_PATH = BASE_PATH + "/step5";

    public static final String STEP_6_INPUT_PATH = STEP_5_OUTPUT_PATH;
    public static final String STEP_6_OUTPUT_PATH = BASE_PATH + "/step6";

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Need to specify data path");
            System.exit(1);
        }

        String dataPath = args[1];

        HdfsDAO hdfs = new HdfsDAO(Recommend.HDFS, config());
        hdfs.rmr(BASE_PATH);


        Step1.run(dataPath);
        Step2.run();
        Step3.run1();
        Step3.run2();

        Step4_Update.run();
        Step4_Update2.run();

        System.exit(0);
    }

    public static JobConf config() {
        JobConf conf = new JobConf(Recommend.class);
        conf.setJobName("Recommend");
        conf.addResource("classpath:/hadoop/core-site.xml");
        conf.addResource("classpath:/hadoop/hdfs-site.xml");
        conf.addResource("classpath:/hadoop/mapred-site.xml");
        return conf;
    }

}