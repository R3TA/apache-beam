package com.demo.beam.app;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.GroupByKey;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.SimpleFunction;
import org.apache.beam.sdk.values.KV;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
    	// We get path about directory beam
    	String beamRoot = System.getProperty("user.dir") + "/src/main/resources/beam/";
    	
    	// First Place we create a new pipeline
    	
    	Pipeline pipeline = Pipeline.create();
    	
    	// Second place we create a new PTransform
    	// pipeline.apply() method which will read data from text file and create a new PCollection of strings. 
    	// To do this, we use one of the already implemented IOs in Beam. TextIO allows to read from and write 
    	// into text file(s) line by line. It has many other features, like working with different file systems, 
    	// supporting file patterns, streaming of files.
    	
    	// The output of this PTransform is a new instance of PCollection<String> where every entry of the collection 
    	// is a text line of input file.
    	
    	pipeline.apply(TextIO.read().from(beamRoot.concat("/leadship.txt")))
    	
    	// Since we want to have the total number of sales per brand as a result, we must group them accordingly. 
    	// Therefore, the next step will be to parse every line and create a key/value pair where key is a brand name 
    	// and value is a number of sales. It's worth to mention that the output PCollection from a previous PTransform 
    	// will be the input PCollection for this one.
    	
    	
    	// In this step, we use Beam internal PTransform, that is called MapElements to create a new pair of key/values 
    	// for every input entry using the provided implementation of SimpleFunction interface.
    	
    	.apply("ParseAndConvertToKV", MapElements.via(
    			new SimpleFunction<String, KV<String, Integer>>(){
    				public KV<String, Integer> apply(String input){
    					String[] split = input.split(",");
    					if(split.length < 4) {
    						return null;
    					}
    					String key = split[1];
    					Integer value = Integer.valueOf(split[3]);
    					return KV.of(key, value);
    				}
    			}
    	))
    	// We then group the number of sales by brand using another Beam's transform - GroupByKey. As an output result we 
    	// have a PCollection of key/values where key is brand name and value is an iterable collection of sales for that brand.
    	
    	.apply(GroupByKey.<String,Integer>create())
    	
    	// Now we are ready to sum up all numbers of car sales per brand using our own implementation of ParDo transform
    	.apply("SumUpValuesByKey", ParDo.of(new DoFn<KV<String, Iterable<Integer>>, String>(){
    		@ProcessElement
    		public void processElement(ProcessContext context) {
    			Integer totalSales = 0;
    			String brand = context.element().getKey();
    			Iterable<Integer> sales = context.element().getValue();
    			for (Integer amount: sales) {
    				totalSales += amount;
    			}
    			context.output(brand + ": " + totalSales);
    		}
    	}))
    	// To finalize the pipeline, we apply another IO transform to take the PCollection of strings and write them in a text file		
    	.apply(TextIO.write().to(beamRoot.concat("sales.txt")).withoutSharding());
    	
    	// The last thing we need to do is run our created pipeline
    	pipeline.run();
    	
    }
}
