package edu.hku.comp7305.group1;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import java.io.IOException;
import java.util.Map;
/**
 * 
 * @author aohuijun
 * Merge the itemCooccurrenceMatrix and splitUserVector by matrix multiplication. 
 */

public class Step3 {

//    public static final String JOB_NAME_1 = "Movie Recommender Step 3_1";
//    public static final String JOB_NAME_2 = "Movie Recommender Step 3_2";

    public static final String JOB_NAME_1 = Recommend.JOB_NAME;
    public static final String JOB_NAME_2 = Recommend.JOB_NAME;

    public static class Step31_UserVectorSplitterMapper extends Mapper<LongWritable, Text, Text, Text> {
        private final static Text k = new Text();
        private final static Text v = new Text();

        /**
         * Split the user-item matrix into individual userVector. 
         * Output format: [itemID	userID: rate]
         */
        @Override
        public void map(LongWritable key, Text values, Context context) throws IOException, InterruptedException {
            String[] tokens = Recommend.DELIMITER.split(values.toString());
            for (int i = 1; i < tokens.length; i++) {
                String[] vector = tokens[i].split(":");
//                int itemID = Integer.parseInt(vector[0]);
                String itemID = vector[0];
                String pref = vector[1];

                k.set(itemID);
                v.set(tokens[0] + ":" + pref);
                context.write(k, v);
            }
        }
    }

    public static void run1(final String input, final String output) throws IOException, InterruptedException, ClassNotFoundException {
//        JobConf conf = Recommend.config();

        Configuration conf = new Configuration();

        HdfsDAO hdfs = new HdfsDAO(Recommend.HDFS, conf);
        hdfs.rmr(output);

        Job job = Job.getInstance(conf, Step3.JOB_NAME_1);
        job.setJarByClass(Step3.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        job.setMapperClass(Step31_UserVectorSplitterMapper.class);

        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        FileInputFormat.setInputPaths(job, new Path(input));
        FileOutputFormat.setOutputPath(job, new Path(output));

//        RunningJob job = JobClient.runJob(conf);
//        while (!job.isComplete()) {
        job.waitForCompletion(true);
//        }
    }

    public static class Step32_CooccurrenceColumnWrapperMapper extends Mapper<LongWritable, Text, Text, IntWritable> {
        private final static Text k = new Text();
        private final static IntWritable v = new IntWritable();

        /**
         * Get similarity.
         */
        @Override
        public void map(LongWritable key, Text values, Context output) throws IOException, InterruptedException {
            String[] tokens = Recommend.DELIMITER.split(values.toString());
            k.set(tokens[0]);
            v.set(Integer.parseInt(tokens[1]));
            output.write(k, v);
        }
    }

    public static void run2(final String input, final String output) throws IOException, InterruptedException, ClassNotFoundException {
//        JobConf conf = Recommend.config();

        Configuration conf = new Configuration();

        HdfsDAO hdfs = new HdfsDAO(Recommend.HDFS, conf);
        hdfs.rmr(output);

        Job job = Job.getInstance(conf, Step3.JOB_NAME_2);
        job.setJarByClass(Step3.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        job.setMapperClass(Step32_CooccurrenceColumnWrapperMapper.class);

        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        FileInputFormat.setInputPaths(job, new Path(input));
        FileOutputFormat.setOutputPath(job, new Path(output));

//        RunningJob job = JobClient.runJob(conf);
//        while (!job.isComplete()) {
//            job.waitForCompletion();
//        }
        job.waitForCompletion(true);
    }
}