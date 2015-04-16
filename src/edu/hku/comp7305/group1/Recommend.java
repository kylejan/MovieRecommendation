package edu.hku.comp7305.group1;


import org.apache.hadoop.mapred.JobConf;

import java.util.Arrays;
import java.util.regex.Pattern;

public class Recommend {

    public static final String HDFS = "hdfs://student3-x1:9000";
    public static final Pattern DELIMITER = Pattern.compile("[\t,]");

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Need to specify `data path` and `output path` in HDFS");
            System.err.println("\tFor example: /MovieRecomDataSource /MovieRecomResult");
            System.err.println("Got " + Arrays.asList(args));
            System.exit(1);
        }

        final String dataPath = HDFS + args[0];
        final String outputPath = HDFS + args[1];

        {
            // Ensure the output is not exists
            HdfsDAO hdfs = new HdfsDAO(Recommend.HDFS, config("MovieRecommender"));
            hdfs.rmr(outputPath);
        }

        final String step1OutputPath = outputPath + "/step1";

        final String step2InputPath = step1OutputPath;
        final String step2OutputPath = outputPath + "/step2";

        final String step3InputPath1 = step1OutputPath;
        final String step3OutputPath1 = outputPath + "/step3_1";

        final String step3InputPath2 = step2OutputPath;
        final String step3OutputPath2 = outputPath + "/step3_2";

        final String step4InputPath1 = step3OutputPath1;
        final String step4InputPath2 = step3OutputPath2;
        final String step4OutputPath = outputPath + "/step4";

        final String step5InputPath = step4OutputPath;
        final String step5OutputPath = outputPath + "/step5";

        Step1.run(dataPath, step1OutputPath);

        Step2.run(step2InputPath, step2OutputPath);

        Step3.run1(step3InputPath1, step3OutputPath1);
        Step3.run2(step3InputPath2, step3OutputPath2);

        Step4.run(step4InputPath1, step4InputPath2, step4OutputPath);

        Step5.run(step5InputPath, step5OutputPath);

        System.exit(0);
    }

    public static JobConf config(final String jobName) {
        JobConf conf = new JobConf(Recommend.class);
        conf.setJobName(jobName);
        conf.addResource("classpath:/hadoop/core-site.xml");
        conf.addResource("classpath:/hadoop/hdfs-site.xml");
        conf.addResource("classpath:/hadoop/mapred-site.xml");
        return conf;
    }

}