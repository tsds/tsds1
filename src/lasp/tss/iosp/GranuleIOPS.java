package lasp.tss.iosp;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Element;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;

/**
 * Read all the contents of a data granule (e.g. file) into a simple data 
 * container, caching the data to satisfy reads without having to delegate
 * to the data source. 
 */
public abstract class GranuleIOPS extends AbstractIOServiceProvider {

    // Initialize a logger.
    private static final Logger _logger = Logger.getLogger(GranuleIOPS.class);
    
    //private HashMap<String,Object> _data = new HashMap<String,Object>();
    
    private RandomAccessFile _raFile;
    private NetcdfFile _ncFile;
    
    /**
     * Number of time samples defined in the time Dimension. "-1" implies undefined.
     * If not defined, the subclass must override getLength() to indicate the number
     * of time samples, presumably after reading them all.
     */
    private int _length = -1;
    
    protected abstract void readAllData();
    protected abstract Array getData(Variable var);
    
    protected int getLength() {
        return _length;
    }
    
    public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
        _raFile = raf;
        _ncFile = ncfile;

        /*
         * TODO: still trying to open for every read when aggregating
         * VariableDS hasCachedData() is false for TBR
         *   this var appears to be the agregated var, not the 2 we cached in
         * delegates to proxy reader (AggregationExisting)
         * 
         * NetcdfDataset.acquireFile
         *   looks for file cache, none, so reads ncml
         *   NetcdfDataset.initNetcdfFileCache
         *      * Enable file caching. call this before calling acquireFile().
         *      * When application terminates, call NetcdfDataset.shutdown().
         */
        
        try {

            readAllData();

            Element ncElement = ncfile.getNetcdfElement();
            makeDimensions(null, ncElement);
            makeVariables(null, ncElement);
            //makeGroups(null, ncElement);
            ncfile.finish(); //doesn't seem to matter
            
        } catch (Throwable t) {
            //TODO: better msg, nc code won't allow us to pass a TSSException
            //  try around each step
            String msg = "IOSP open failed.";
            _logger.error(msg, t);
            throw new IOException(msg, t);
        }
        
    }
    
//    protected void addData(String varName, String value) {
//        Object o = _data.get(varName);
//        if (o == null) {
//            if (getVariable(varName).getDataType().isString()) {
//                o = new ArrayList<String>();
//            } else {
//                o = new ResizableDoubleArray();
//            }
//            
//            _data.put(varName, o);
//        }
//        
//        if (getVariable(varName).getDataType().isString()) {
//            ((List) o).add(value);
//        } else {
//            double d = Double.parseDouble(value);
//            //TODO: if fails, set to NaN
//            ((ResizableDoubleArray) o).addElement(d);
//        }
//    }
    
    //TODO: setData(vname, array) all at once instead of add datum
    

    /**
     * Get the value of the property with the given name.
     * These properties are defined as XML attributes 
     * of the "netcdf" element.
     */
    protected String getProperty(String name) {
        String prop = null;

        Element ncElement = getNetcdfFile().getNetcdfElement();
        prop = ncElement.getAttributeValue(name); //from "netcdf" ncml Element

        return prop;
    }
    
    /**
     * Return the file that NetCDF handed us in the call to "open" (already open).
     */
    public RandomAccessFile getFile() {
        return _raFile;
    }
    



    /**
     * Return the NetcdfFile that was given to us and populated by in in the "open" method.
     */
    protected NetcdfFile getNetcdfFile() {
        return _ncFile;
    }
    
//    protected List<Variable> getVariables() {
//        return _ncFile.getRootGroup().getVariables();
//    }
//    
//    protected Variable getVariable(String varName) {
//        return _ncFile.findVariable(varName);
//    }

    /**
     * Return the name of a variable from the NcML element that defines it.
     * Use the original name if we have it.
     */
    protected String getVariableName(Element varElement) {
        String name = varElement.getAttributeValue("orgName"); 
        if (name == null) name = varElement.getAttributeValue("name");
        return name;
    }
    
/////////////////
    
    /**
     * Look for dimension definitions in the ncml. Create them and add them to ncfile.
     */
    protected void makeDimensions(Group parent, Element element) {
        List<Element> dims = element.getChildren("dimension", element.getNamespace());
        for (Element e : dims) {
            Dimension dim = makeDimension(e);
            _ncFile.addDimension(parent, dim);
        }
    }

    /**
     * Create a Dimension from the XML element that defines it.
     */
    protected Dimension makeDimension(Element element) {
        Dimension dim = null;
        boolean isShared = true; 
       
        boolean isUnlimited = false;
        String unlimited = element.getAttributeValue("isUnlimited");
        if (unlimited != null) isUnlimited = Boolean.parseBoolean(unlimited);
        
        boolean isVariableLength = false;
        String varlen = element.getAttributeValue("isVariableLength");
        if (varlen != null) isVariableLength = Boolean.parseBoolean(varlen);
        
        int n = 0; //default to zero length, 
        String length = element.getAttributeValue("length");
        if (length != null) n = Integer.parseInt(length);

        String name = element.getAttributeValue("name");
        //if (isUnlimited && n == 0) n = getLength();
        
        //If length is not defined and this is the time dimension,
        //  delegate to getLength() to get the number of time samples.
        //This may involve reading all the time data.
        if ("time".equals(name)) {
            if (isVariableLength) {
                n = -1; //unlimited (or unknown?), not used/tested
            } else if (n == 0) { //not variable length and not defined in ncml
                n = getLength(); //get length from other method (e.g. read times) and save it
                _length = n;
            }
        }

        dim = new Dimension(name, n, isShared, isUnlimited, isVariableLength);

        return dim;
    }


    /**
     * Look for variable definitions in the ncml. Create them and add them to ncfile.
     */
    protected void makeVariables(Group parent, Element element) {
        List<Element> vars = element.getChildren("variable", element.getNamespace());

        for (Element e : vars) {
            Variable var = makeVariable(parent, e);
            _ncFile.addVariable(parent, var);
        }   
    }
    
    /**
     * Create a Variable with the given parent and the XML element that defines it.
     */
    protected Variable makeVariable(Group parent, Element element) {
        Variable var = null;
        
        String vname = getVariableName(element);
        String shape = element.getAttributeValue("shape"); //may be null for scalar
        String type = element.getAttributeValue("type");
        //TODO: do we need to worry about Structure type?

        var = new Variable(_ncFile, parent, null, vname);
        var.setDataType(DataType.getType(type));
        var.setDimensions(shape); //Var gets shape (int[]) set here, changing dim size later won't change it
        
        //add attributes
        List<Element> atts = element.getChildren("attribute", element.getNamespace());
        for (Element attel : atts) {
            String name = attel.getAttributeValue("name");
            String value = attel.getAttributeValue("value");
            Attribute att = new Attribute(name, value);
            var.addAttribute(att);
        }
        
        //cache the data
        Array array = getData(var);
        var.setCachedData(array, false);
        
        return var;
    }
    
    
    /**
     * Return the value of a given named attribute from the variable with the given name.
     */
    protected String getVariableXmlAttribute(String varName, String attName) {
        String s = null;
        
        Element element = getNetcdfFile().getNetcdfElement();
        Iterator it = (Iterator) element.getDescendants();
        while (it.hasNext()) {
            Object o = it.next();
            if (! (o instanceof Element)) continue;
            Element e = (Element) o;
            String ename = e.getName();
            if (! ename.equals("variable")) continue;
            
            String vname = e.getAttributeValue("name");
            if (vname.equals(varName)) {
                s = e.getAttributeValue(attName);
                break;
            }
        }
        
        return s;
    }
    
    
    /**
     * No-op. Shouldn't be needed since we are using the Variable cache.
     */
    @Override
    public Array readData(Variable variable, Section section) throws IOException, InvalidRangeException {
        System.out.println("[WARN] Why is NetCDF trying to read me?");
        return null;
    }

    /**
     * Release resources used by this IOSP.
     * IMPORTANT: Override this if you use resources in addition to the "location" file.
     *   Note that NetCDF has already opened the "location" file so it needs to be closed.
     *   Otherwise, you may see "too many open files" type errors in your Servlet container.
     */
    public void close() throws IOException {
        try {
            getFile().close();
        } catch (IOException e) {
            String msg = "Failed closing data source: " + getFile().getLocation();
            _logger.warn(msg, e);
            throw e;
        }
    }
    
    //----- Implement interface methods so the Reader author doesn't have to. -----//

    /**
     * Return false so NetCDF won't automatically determine that this IOSP should handle the given file.
     * Use this IOSP only if it is specified in the ncml with the "iosp" attribute.
     */
    public boolean isValidFile(RandomAccessFile raf) throws IOException {
        return false;
    }
    
    @Override
    public String getFileTypeDescription() {
        return "Generic Granule";
    }

    @Override
    public String getFileTypeId() {
        return "TSS-Granule";
    }

}
