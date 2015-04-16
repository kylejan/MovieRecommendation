package edu.hku.comp7305.group1;

/**
 * Created by zonyitoo on 14/4/15.
 */

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
/**
 * 
 * @author aohuijun
 * Use user_ID as the indicator and map the <item_ID, rate> accordingly. 
 */
public class Step1 {

    public static class Step1_ToItemPreMapper extends MapReduceBase implements Mapper<Object, Text, IntWritable, Text> {
    	/**
    	 * Map to get the pair of (user, <itemID: rate>).
    	 */
        private final static IntWritable k = new IntWritable();
        private final static Text v = new Text();									// The class Text is like Object.

        @Override
        public void map(Object key, Text value, OutputCollector<IntWritable, Text> output, Reporter reporter) throws IOException {
            String[] tokens = Recommend.DELIMITER.split(value.toString());			// Split all the records by using the parameter in Pattern.split(regex). 
            int userID = Integer.parseInt(tokens[0]);								// In one token, the first item that is split out is the userID.
            String itemID = tokens[1];												// Then the second one is itemID.
            String pref = tokens[2];												// The last one is the preference(rate). 
            k.set(userID);
            v.set(itemID + ":" + pref);
            output.collect(k, v);
        }
    }

    public static class Step1_ToUserVectorReducer extends MapReduceBase implements Reducer<IntWritable, Text, IntWritable, Text> {
        private final static Text v = new Text();

        /**
         * Get the mapper's result to construct the reduce result. 
         */
        @Override
        public void reduce(IntWritable intWritable, Iterator<Text> iterator, OutputCollector<IntWritable, Text> outputCollector, Reporter reporter) throws IOException {
            StringBuilder sb = new StringBuilder();									// Like "string + string" but with higher efficiency. 
            while (iterator.hasNext()) {
                sb.append("," + iterator.next());									// Append the <item, rate> pair that with the same userID. 
            }
            v.set(sb.toString().replaceFirst(",", ""));								// Delete the first ",". 
            outputCollector.collect(intWritable, v);
        }
    }

    /**
     * FIRST ROUND MAPREDUCE EXECUTION: run for getting the whole user-item matrix. 
     */
    public static void run(final String input, final String output) throws IOException {
        JobConf conf = Recommend.config();

        HdfsDAO hdfs = new HdfsDAO(Recommend.HDFS, conf);
        hdfs.rmr(output);

        conf.setMapOutputKeyClass(IntWritable.class);
        conf.setMapOutputValueClass(Text.class);

        conf.setOutputKeyClass(IntWritable.class);
        conf.setOutputValueClass(Text.class);

        conf.setMapperClass(Step1_ToItemPreMapper.class);
        conf.setCombinerClass(Step1_ToUserVectorReducer.class);
        conf.setReducerClass(Step1_ToUserVectorReducer.class);

        conf.setInputFormat(TextInputFormat.class);
        conf.setOutputFormat(TextOutputFormat.class);

        FileInputFormat.setInputPaths(conf, new Path(input));
        FileOutputFormat.setOutputPath(conf, new Path(output));

        RunningJob job = JobClient.runJob(conf);
        while (!job.isComplete()) {
            job.waitForCompletion();
        }
    }

}
