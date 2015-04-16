package edu.hku.comp7305.group1;

/**
 * Created by zonyitoo on 14/4/15.
 */

import org.apache.commons.lang.UnhandledException;
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
import java.util.Iterator;
import java.util.Map;
/**
 * 
 * @author aohuijun
 * Use user_ID as the indicator and map the <item_ID, rate> accordingly. 
 */
public class Step1 {

    public static final String JOB_NAME = "Movie Recommender Step 1";

    public static class Step1_ToItemPreMapper extends Mapper<Object, Text, Text, Text> {
    	/**
    	 * Map to get the pair of (user, <itemID: rate>).
    	 */
        private final static Text k = new Text();
        private final static Text v = new Text();									// The class Text is like Object.

        @Override
        public void map(Object key, Text value, Context output) throws IOException, InterruptedException {
            String[] tokens = Recommend.DELIMITER.split(value.toString());			// Split all the records by using the parameter in Pattern.split(regex). 
            String userID = tokens[0];								// In one token, the first item that is split out is the userID.
            String itemID = tokens[1];												// Then the second one is itemID.
            String pref = tokens[2];												// The last one is the preference(rate). 
            k.set(userID);
            v.set(itemID + ":" + pref);
            output.write(k, v);
        }
    }

    public static class Step1_ToUserVectorReducer extends Reducer<Text, Text, Text, Text> {
        private final static Text v = new Text();

        @Override
        protected void reduce(Text userID, Iterable<Text> iterator, Context context) throws IOException, InterruptedException {
            StringBuilder sb = new StringBuilder();									// Like "string + string" but with higher efficiency.
            for (Text x : iterator) {
                sb.append("," + x);									// Append the <item, rate> pair that with the same userID.
            }
            v.set(sb.toString().replaceFirst(",", ""));								// Delete the first ",".
            context.write(userID, v);
        }
    }

    /**
     * FIRST ROUND MAPREDUCE EXECUTION: run for getting the whole user-item matrix. 
     */
    public static void run(final String input, final String output) throws IOException, InterruptedException, ClassNotFoundException {
        Configuration conf = new Configuration();
//        JobConf conf = Recommend.config();


        HdfsDAO hdfs = new HdfsDAO(Recommend.HDFS, conf);
        hdfs.rmr(output);

        Job job = Job.getInstance(conf, Step1.JOB_NAME);
        job.setJarByClass(Step1.class);

        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        job.setMapperClass(Step1_ToItemPreMapper.class);
        job.setCombinerClass(Step1_ToUserVectorReducer.class);
        job.setReducerClass(Step1_ToUserVectorReducer.class);

        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        FileInputFormat.setInputPaths(job, new Path(input));
        FileOutputFormat.setOutputPath(job, new Path(output));

//        RunningJob job = JobClient.runJob(conf);
//        while (!job.isComplete()) {
        job.waitForCompletion(true);
//        }
    }

}
