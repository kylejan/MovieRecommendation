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
/**
 * 
 * @author aohuijun
 * Switch the previous jobs into: 
 * 		one set of vectors: the itemCooccurrenceMatrix, and 
 * 		one vector: splitUserVector comes from user-item matrix. 
 */

public class Step3 {

    public static final String JOB_NAME_1 = "Movie Recommender Step 3_1";
    public static final String JOB_NAME_2 = "Movie Recommender Step 3_2";

    public static class Step31_UserVectorSplitterMapper extends Mapper<Text, Text, Text, Text> {
        private final static Text k = new Text();
        private final static Text v = new Text();

        /**
         * Split the user-item matrix into individual userVector. 
         * Result: 
         * 			itemID1		userID1:score1
         * 						userID2:score2
         * 						userID5:score5
         * 						...
         * 			itemID2		...
         * 			...
         */
        @Override
        public void map(Text key, Text values, Context context) throws IOException, InterruptedException {
            String[] tokens = Recommend.DELIMITER.split(values.toString());
            for (int i = 1; i < tokens.length; i++) {
                String[] vector = tokens[i].split(":");
                String itemID = vector[0];
                String score = vector[1];

                k.set(itemID);
                v.set(tokens[0] + ":" + score);
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

        job.setOutputKeyClass(IntWritable.class);
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
         * Get and wrap the similarity result, .
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