package edu.hku.comp7305.group1;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
/**
 * 
 * @author aohuijun
 * Use the userVectors(user-item matrix) to get itemCooccurRenceMatrix. 
 */
public class Step2 {

    public static final String JOB_NAME = "Movie Recommender Step 2";

    public static class Step2_UserVectorToCooccurrenceMapper extends Mapper<LongWritable, Text, Text, IntWritable> {
        private final static Text k = new Text();
        private final static IntWritable v = new IntWritable(1);

        /**
         * Map by calculating the similarity between two items. 
         */
        @Override
        public void map(LongWritable key, Text values, Context output) throws IOException, InterruptedException {
            String[] tokens = Recommend.DELIMITER.split(values.toString());
            for (int i = 1; i < tokens.length; i++) {
                String itemID = tokens[i].split(":")[0];						// Identifying the ":" to get items-pairs. 
                for (int j = 1; j < tokens.length; j++) {
                    String itemID2 = tokens[j].split(":")[0];
                    k.set(itemID + ":" + itemID2);
                    output.write(k, v);
                }
            }
        }
    }

    public static class Step2_UserVectorToCooccurrenceReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
        private IntWritable result = new IntWritable();

        /**
         * Reduce by using mapper's result.
         */
        @Override
        public void reduce(Text key, Iterable<IntWritable> values, Context output) throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable value : values) {
                sum += value.get();
            }
            result.set(sum);
            output.write(key, result);
        }
    }

    /**
     * SECOND ROUND MAPREDUCE EXECUTION: use user-item matrix to get the entire co-occurrence matrix.
     * 
     * 
     */
    public static void run(final String input, final String output) throws IOException, InterruptedException, ClassNotFoundException {
        Configuration conf = new Configuration();

//        JobConf conf = Recommend.config();

        HdfsDAO hdfs = new HdfsDAO(Recommend.HDFS, conf);
        hdfs.rmr(output);

        Job job = Job.getInstance(conf, Step2.JOB_NAME);
        job.setJarByClass(Step2.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        job.setMapperClass(Step2_UserVectorToCooccurrenceMapper.class);
        job.setCombinerClass(Step2_UserVectorToCooccurrenceReducer.class);
        job.setReducerClass(Step2_UserVectorToCooccurrenceReducer.class);

        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        FileInputFormat.setInputPaths(job, new Path(input));
        FileOutputFormat.setOutputPath(job, new Path(output));

//        RunningJob job = JobClient.runJob(conf);
        job.waitForCompletion(true);
    }
}
