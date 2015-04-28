package edu.hku.comp7305.group1;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import java.io.IOException;
/**
 * 
 * @author aohuijun
 * Use the userVectors(user-item matrix) to get the similarity matrix called itemCooccurrenceMatrix. 
 */
public class Step2 {

    public static final String JOB_NAME = "Movie Recommender Step 2";
//    public static final String JOB_NAME = Recommend.JOB_NAME;

    public static class Step2_UserVectorToCooccurrenceMapper extends Mapper<Object, Text, Text, IntWritable> {
        private final static Text k = new Text();
        private final static IntWritable v = new IntWritable(1);

        /**
         * Calculating situation: get a pair of two items that show up in one user's user-item matrix. 
         * Result (for each key: userID): 
         * 			itemID1:itemID2	1
         * 			itemID1:itemID2	1
         * 			itemID1:itemTD3	1
         * 			itemID3:itemID4	1
         * 			...
         */
        @Override
        public void map(Object key, Text values, Context output) throws IOException, InterruptedException {
            String[] tokens = Recommend.DELIMITER.split(values.toString());
            for (int i = 1; i < tokens.length; i++) {
                String itemID = tokens[i].split(":")[0];						// Identifying the ":" to get items-pairs. 
                for (int j = 1; j < tokens.length; j++) {
                    String itemID2 = tokens[j].split(":")[0];
                    k.set(itemID + ":" + itemID2);								// Get the pair: <itemID:itemID2> that under the name of the same userID. 
                    output.write(k, v);
                }
            }
        }
    }

    public static class Step2_UserVectorToCooccurrenceReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
        private IntWritable result = new IntWritable();

        /**
         * Reduce out the similarity matrix by using all the item pairs results.
         * Result (regardless the userID): 
         * 			itemID1:itemID2	(sum1)
         * 			itemID1:itemID3	(sum2)
         * 			itemID2:itemID3 (sum3)
         * 			...
         * (Notice: the new key is the items pair while the value is the co-occurrence number. )
         */
        @Override
        public void reduce(Text key, Iterable<IntWritable> values, Context output) throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable value : values) {
                sum += value.get();
            }
            result.set(sum);													// Calculate the sum of the 1s of all same item pairs<itemID:itemID2>.
            output.write(key, result);
        }
    }

    /**
     * SECOND ROUND MAPREDUCE EXECUTION: use user-item matrix to get the entire co-occurrence matrix.
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

        job.setNumReduceTasks(7);

        FileInputFormat.setInputPaths(job, new Path(input));
        FileOutputFormat.setOutputPath(job, new Path(output));

//        RunningJob job = JobClient.runJob(conf);
        job.waitForCompletion(true);
    }
}
