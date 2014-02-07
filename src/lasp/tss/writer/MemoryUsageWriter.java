package lasp.tss.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Writer will print out the memory usage before and after 
 * the dataset has been loaded.
 * Designed for diagnostic use.
 */
public class MemoryUsageWriter extends TextDataWriter {

    // Initialize a logger.
    private static final Logger _logger = LoggerFactory.getLogger(MemoryUsageWriter.class);
    
    private long beforeMemory;
    
    /**
     * Use constructor to set the before memory.
     * Note, init is called after dataset has been constructed, so not useful for 'before'.
     */
    public MemoryUsageWriter() {
        beforeMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    /**
     * Write the memory usage instead of the dataset contents.
     */
    public void write() {
        long max = Runtime.getRuntime().maxMemory();
        long current = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        
       
        println("Memory usage for dataset: " + getDataset().getName());
        println("Before: " + beforeMemory +"/"+ max);
        println("After : " + current +"/"+ max);
        
        String msg = "Memory usage for dataset " + getDataset().getName() 
        		+ " Before: " + beforeMemory +"/"+ max 
        		+ " After: " + current +"/"+ max;
        println();
        println(msg);
        _logger.info(msg);
    }

    //no-op impl of abstract method
    public void writeTimeSample(int timeIndex) {}
}
