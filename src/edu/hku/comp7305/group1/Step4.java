package edu.hku.comp7305.group1;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
/**
 * 
 * @author aohuijun
 * Use user_ID as the indicator and map the <item_ID, rate> accordingly. 
 */
public class Step4 {

    public static final String JOB_NAME = "Movie Recommender Step 4";
//    public static final String JOB_NAME = Recommend.JOB_NAME;

    public static class Step4_PartialMultiplyMapper extends Mapper<LongWritable, Text, Text, Text> {

        private String flag;													// Set it as the indicator of co-occurrence matrix or userVector. 

        @Override
        protected void setup(Context context) throws IOException, InterruptedException {
            FileSplit split = (FileSplit) context.getInputSplit();
            flag = split.getPath().getParent().getName();						// Get the name of the input file. 

            // System.out.println(flag);
        }

        /**
         * Map for further use in multiply. 
         * TO distinguish the co-occurrence matrix or the userVector separately. 
         * 
         */
        @Override
        public void map(LongWritable key, Text values, Context context) throws IOException, InterruptedException {
            String[] tokens = Recommend.DELIMITER.split(values.toString());

            if (flag.equals("step3_2")) {										// The input is the co-occurrence matrix. 
                String[] v1 = tokens[0].split(":");
                String itemID1 = v1[0];
                String itemID2 = v1[1];
                String num = tokens[1];

                Text k = new Text(itemID1);
                Text v = new Text("Similarity:" + itemID2 + "," + num);

                context.write(k, v);
//                System.out.println(k.toString() + "  " + v.toString());

            } else if (flag.equals("step3_1")) {								// The input is the userVector instead. 
                String[] v2 = tokens[1].split(":");
                String itemID = tokens[0];
                String userID = v2[0];
                String pref = v2[1];

                Text k = new Text(itemID);
                Text v = new Text("User:" + userID + "," + pref);

                context.write(k, v);
//                System.out.println(k.toString() + "  " + v.toString());
            }
        }

    }

    public static class Step4_AggregateReducer extends Reducer<Text, Text, Text, Text> {

    	/**
    	 * Get multiplied result. 
    	 * Result: 
    	 * 			userID1		itemID1,recommendation1
    	 * 			userID2		itemID2,recommendation2
    	 * 			userID1		itemID1,recommendation3
    	 * 						...
    	 * 			userID2		itemID1,recommendation1
    	 * 			...
    	 */
        @Override
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            System.out.println(key.toString() + ":");

            Map<String, String> mapA = new HashMap<String, String>();			// Map A is for the co-occurrence matrix. 
            Map<String, String> mapB = new HashMap<String, String>();			// Map B is for the userVector. 

            for (Text line : values) {
                String val = line.toString();
                System.out.println(val);

                if (val.startsWith("Similarity:")) {
                    String[] kv = Recommend.DELIMITER.split(val.substring(11));
                    mapA.put(kv[0], kv[1]);

                } else if (val.startsWith("User:")) {
                    String[] kv = Recommend.DELIMITER.split(val.substring(5));
                    mapB.put(kv[0], kv[1]);
                }
            }

            double result = 0;
            Iterator<String> iter = mapA.keySet().iterator();
            while (iter.hasNext()) {
                String mapk = iter.next();									// itemID

                int num = Integer.parseInt(mapA.get(mapk));
                Iterator<String> iterb = mapB.keySet().iterator();
                while (iterb.hasNext()) {
                    String mapkb = iterb.next();							// userID
                    double pref = Double.parseDouble(mapB.get(mapkb));
                    result = num * pref;									// Get all the multiplied results but without adding them together.

                    Text k = new Text(mapkb);
                    Text v = new Text(mapk + "," + result);
                    context.write(k, v);
//                    System.out.println(k.toString() + "  " + v.toString());
                }
            }
        }
    }

    public static void run(final String input1, final String input2, final String output)
            throws IOException, InterruptedException, ClassNotFoundException {
//        JobConf conf = Recommend.config();

        Configuration conf = new Configuration();

        HdfsDAO hdfs = new HdfsDAO(Recommend.HDFS, conf);
        hdfs.rmr(output);

//        Job job = new Job(conf);

        Job job = Job.getInstance(conf, Step4.JOB_NAME);
        job.setJarByClass(Step4.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        job.setMapperClass(Step4.Step4_PartialMultiplyMapper.class);
        job.setReducerClass(Step4.Step4_AggregateReducer.class);

        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        job.setNumReduceTasks(7);

        FileInputFormat.setInputPaths(job, new Path(input1), new Path(input2));
        FileOutputFormat.setOutputPath(job, new Path(output));

        job.waitForCompletion(true);
    }

}
