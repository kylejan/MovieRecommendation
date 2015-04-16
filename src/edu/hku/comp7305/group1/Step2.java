package edu.hku.comp7305.group1;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
/**
 * 
 * @author aohuijun
 * Use the userVectors(user-item matrix) to get itemCooccurRenceMatrix. 
 */
public class Step2 {
    public static class Step2_UserVectorToCooccurrenceMapper extends MapReduceBase implements Mapper<LongWritable, Text, Text, IntWritable> {
        private final static Text k = new Text();
        private final static IntWritable v = new IntWritable(1);

        /**
         * Map by calculating the similarity between two items. 
         */
        @Override
        public void map(LongWritable key, Text values, OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
            String[] tokens = Recommend.DELIMITER.split(values.toString());
            for (int i = 1; i < tokens.length; i++) {
                String itemID = tokens[i].split(":")[0];						// Identifying the ":" to get items-pairs. 
                for (int j = 1; j < tokens.length; j++) {
                    String itemID2 = tokens[j].split(":")[0];
                    k.set(itemID + ":" + itemID2);
                    output.collect(k, v);
                }
            }
        }
    }

    public static class Step2_UserVectorToCooccurrenceReducer extends MapReduceBase implements Reducer<Text, IntWritable, Text, IntWritable> {
        private IntWritable result = new IntWritable();

        /**
         * Reduce by using mapper's result.
         */
        @Override
        public void reduce(Text key, Iterator<IntWritable> values, OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
            int sum = 0;
            while (values.hasNext()) {
                sum += values.next().get();
            }
            result.set(sum);
            output.collect(key, result);
        }
    }

    /**
     * SECOND ROUND MAPREDUCE EXECUTION: use user-item matrix to get the entire co-occurrence matrix.
     * 
     * 
     */
    public static void run(final String input, final String output) throws IOException {
        JobConf conf = Recommend.config("MovieRecommender Step2");

        HdfsDAO hdfs = new HdfsDAO(Recommend.HDFS, conf);
        hdfs.rmr(output);

        conf.setOutputKeyClass(Text.class);
        conf.setOutputValueClass(IntWritable.class);

        conf.setMapperClass(Step2_UserVectorToCooccurrenceMapper.class);
        conf.setCombinerClass(Step2_UserVectorToCooccurrenceReducer.class);
        conf.setReducerClass(Step2_UserVectorToCooccurrenceReducer.class);

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
