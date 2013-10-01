package lasp.tss.writer;

/**
 * This Writer will print out the memory usage before and after 
 * the dataset has been loaded.
 * Designed for diagnostic use.
 */
public class MemoryUsageWriter extends TextDataWriter {

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
    }

    //no-op impl of abstract method
    public void writeTimeSample(int timeIndex) {}
}
