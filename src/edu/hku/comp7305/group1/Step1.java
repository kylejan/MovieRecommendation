package edu.hku.comp7305.group1;

/**
 * Created by zonyitoo on 14/4/15.
 */

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
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
 * Use userID as the indicator and map the <itemID, score> accordingly. 
 */
public class Step1 {

    public static final String JOB_NAME = "Movie Recommender Step 1";
//    public static final String JOB_NAME = Recommend.JOB_NAME;

    public static class Step1_ToItemPreMapper extends Mapper<Object, Text, Text, Text> {
    	/**
    	 * Map the initial income. 
    	 * Result: get all the pairs of (userID, <itemID: score>).
    	 */
        private final static Text k = new Text();
        private final static Text v = new Text();									// The class Text is like Object.

        /**
         * Called once for each key/value pair in the input split.
         */
        @Override
        public void map(Object key, Text value, Context output) throws IOException, InterruptedException {
            String[] tokens = Recommend.DELIMITER.split(value.toString());			// Split all the records by using the parameter in Pattern.split(regex). 
            String userID = tokens[0];												// In one token, the first item that is split out is the userID.
            String itemID = tokens[1];												// Then the second one is itemID.
            String score = tokens[2];												// The last one is the score. 
            k.set(userID);
            v.set(itemID + ":" + score);
            output.write(k, v);
        }
    }

    public static class Step1_ToUserVectorReducer extends Reducer<Text, Text, Text, Text> {
        private final static Text v = new Text();

        /**
         * Get the mapper's result to construct the reduce result. 
         * Result: a matrix that recorded all the <itemID: score> pairs with the according userID. 
         * 	for example: 
         * 			userID1				itemID1:score1	itemID2:score2	...
         * 			userID2				itemID1:score1	itemID3:score3	...
         * 			...
         * 
         */
        @Override
        protected void reduce(Text userID, Iterable<Text> iterator, Context context) throws IOException, InterruptedException {
            StringBuilder sb = new StringBuilder();									// Like "string + string" but with higher efficiency.
            for (Text x : iterator) {
                sb.append("," + x);													// Append the <item, rate> pair that with the same userID.
            }
            v.set(sb.toString().replaceFirst(",", ""));								// Delete the first ",".
            context.write(userID, v);
        }
    }

    /**
     * FIRST ROUND MAPREDUCE EXECUTION: run for getting the reduced matrix called user-item matrix. 
     */
    public static void run(final String input, final String output) throws IOException, InterruptedException, ClassNotFoundException {
        Configuration conf = new Configuration();
//        JobConf conf = Recommend.config();

        HdfsDAO hdfs = new HdfsDAO(Recommend.HDFS, conf);
        hdfs.rmr(output);

        Job job = Job.getInstance(conf, Step1.JOB_NAME);
        job.setJarByClass(Step1.class);								// Set the job jar.

        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        job.setMapperClass(Step1_ToItemPreMapper.class);			// Here to begin the map-reduce procedures. 
        job.setCombinerClass(Step1_ToUserVectorReducer.class);
        job.setReducerClass(Step1_ToUserVectorReducer.class);

        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        job.setNumReduceTasks(7);

        FileInputFormat.setInputPaths(job, new Path(input));
        FileOutputFormat.setOutputPath(job, new Path(output));

//        RunningJob job = JobClient.runJob(conf);
//        while (!job.isComplete()) {
        job.waitForCompletion(true);
//        }
    }

}
