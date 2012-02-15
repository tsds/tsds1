package lasp.tss.iosp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.xpath.XPath;

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
public abstract class GranuleIOSP extends AbstractIOServiceProvider {

    // Initialize a logger.
    private static final Logger _logger = Logger.getLogger(GranuleIOSP.class);
    
    private HashMap<String, Array> _dataMap = new HashMap<String, Array>();
    
    private RandomAccessFile _raFile;
    private NetcdfFile _ncFile;
    
    /**
     * Number of time samples defined in the time Dimension.
     * If not defined in the NcML, the subclass must override getLength() to indicate the number
     * of time samples, presumably after reading them all.
     */
    private int _length = -1;
    
    protected abstract void readAllData();

    //TODO; double[] getData(varName)    
    protected Array getData(Variable var) {
        String vname = var.getShortName();
        Array array = getArray(vname);
        return array;
    }
    
    /**
     * Hook to allow subclasses to do some initialization before reading data.
     */
    protected void init() {}
    
    protected int getLength() {
        return _length;
    }
    
    protected void setLength(int length) {
        _length = length;
    }
    
    public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
        _raFile = raf;
        _ncFile = ncfile;
        
        /*
         * TODO: try caching the dataMap?
         * need to worry about expiration...
         * Does FileCache persist between requests?
         */
        
        
        try {
            init();
            
            Element ncElement = ncfile.getNetcdfElement();
            makeDimensions(null, ncElement);
            makeVariables(null, ncElement);
            makeGroups(null, ncElement);
            
            readAllData();
            
            //Set the length of the time dimension.
            Dimension tdim = _ncFile.getRootGroup().findDimension("time");
            int n = getLength();
            tdim.setLength(n);

            //Tell each Variable what it's shape is.
            //This has to be done after the Dimension lengths have been set.
            //Then put the data in the Variable cache.
            List<Variable> vars = getVariables();
            for (Variable var : vars) {
                String shape = getVariableXmlAttribute(var.getShortName(), "shape");
                var.setDimensions(shape);

                Array array = getData(var);
                var.setCachedData(array, false);
            }
            
            ncfile.finish(); //doesn't seem to matter
            
        } catch (Throwable t) {
            //TODO: better msg, nc code won't allow us to pass a TSSException
            //  try around each step
            String msg = "IOSP open failed.";
            _logger.error(msg, t);
            throw new IOException(msg, t);
        }
        
    }
    

    protected Array getArray(String varName) {
        return _dataMap.get(varName);
    }
    
    protected void setArray(String varName, Array array) {
        _dataMap.put(varName, array);
    }
    
//explore other ways for subclasses to load the data
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
    
    protected String getQuery() {
        String query = getProperty("query");
        try {
            query = URLDecoder.decode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return query;
    }
    
    /**
     * Return the file that NetCDF handed us in the call to "open" (already open).
     */
    protected RandomAccessFile getFile() {
        return _raFile;
    }

    /**
     * Return the URL for the data granule.
     * Try looking for a "url" definition,
     * else get it from the "location".
     */
    protected String getURL() {
        String url = getProperty("url");
        if (url == null && _raFile != null) {
            url = _raFile.getLocation();
        }

        return url;
    }

    /**
     * Return the NetcdfFile that was given to us and populated by in in the "open" method.
     */
    protected NetcdfFile getNetcdfFile() {
        return _ncFile;
    }

    protected List<Variable> getVariables() {
        
        //assumes model has been defined
        List<Variable> vars = new ArrayList<Variable>(); //start with empty List
        vars = gatherVariables(_ncFile.getRootGroup(), vars); //recursively get Variables in all Groups
        
        return vars;
    }
    
    private List<Variable> gatherVariables(Group group, List<Variable> variables) {
        List<Variable> vars = group.getVariables();
        variables.addAll(vars);
        
        for (Group g : group.getGroups()) {
            variables = gatherVariables(g, variables); //recursive
        }
        
        return variables;
    }
    
//    /**
//     * Return a List of Variable names to be used as keys in the data Map.
//     * Get them from the NcML since the NetcdfFile might not be defined yet.
//     */
//    protected List<String> getVariableNames() {
//        //TODO: only used by RegEx reader to get nvar, just make getVariableCount?
//        List<String> names = new ArrayList<String>();
//        
//        Element element = _ncFile.getNetcdfElement();
//        List<Element> vars = element.getChildren("variable", element.getNamespace());
//
//        for (Element e : vars) {
//            String name = e.getAttributeValue("name");
//            names.add(name);
//        }   
//        
//        return names;
//    }
    
    protected List<Element> getVariableElements() {
        List<Element> vars = null;
        
        try {
            //Use XPath query
            Element element = getNetcdfFile().getNetcdfElement();
            Namespace ns = element.getNamespace();
            String ns_prefix = ns.getPrefix();
            if (ns_prefix.equals("")) ns_prefix = "dummy"; //XPath doesn't support a default namespace
            String ns_uri = ns.getURI();

            String q = "//" + ns_prefix + ":variable";
            XPath x = XPath.newInstance(q);
            x.addNamespace(ns_prefix, ns_uri);
            vars = x.selectNodes(element);

        } catch (JDOMException e) {
            e.printStackTrace();
        }
        
        return vars;
    }    
    
    protected int getVariableCount() {
        //TODO: is this consistent with getVariables? i.e. all nested variables
        int nvar = getVariableElements().size();
        return nvar;
    }

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
        String name = element.getAttributeValue("name");
        
        boolean isShared = true; 
       
        boolean isUnlimited = false;
        String unlimited = element.getAttributeValue("isUnlimited");
        if (unlimited != null) isUnlimited = Boolean.parseBoolean(unlimited);
        
        boolean isVariableLength = false;
        String varlen = element.getAttributeValue("isVariableLength");
        if (varlen != null) isVariableLength = Boolean.parseBoolean(varlen);
        
        int n = 0;
        String length = element.getAttributeValue("length");
        if (length != null) {
            n = Integer.parseInt(length);
            if (name.equals("time")) _length = n;
        }
        
        //TODO: error if length 0 and not unlimited?

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
        //String shape = element.getAttributeValue("shape"); //may be null for scalar
        String type = element.getAttributeValue("type");
        //TODO: do we need to worry about Structure type?

        var = new Variable(_ncFile, parent, null, vname);
        var.setDataType(DataType.getType(type));
        //var.setDimensions(shape); do after we know the time dim length, can't reset var's shape
        
        //add attributes
        List<Element> atts = element.getChildren("attribute", element.getNamespace());
        for (Element attel : atts) {
            String name = attel.getAttributeValue("name");
            String value = attel.getAttributeValue("value");
            Attribute att = new Attribute(name, value);
            var.addAttribute(att);
        }
        
        return var;
    }
    
    /**
     * Look for group definitions in the ncml. Create them (without data) and add them to ncfile.
     */
    protected void makeGroups(Group parent, Element element) {
        List<Element> groups = element.getChildren("group", element.getNamespace());
        for (Element e : groups) {
            Group group = makeGroup(parent, e);
            _ncFile.addGroup(parent, group);
        }
    }
    
    /**
     * Create a Group with the given parent and the XML element that defines it.
     */
    protected Group makeGroup(Group parent, Element element) {
        Group group = null;
        
        String name = element.getAttributeValue("name");
        group = new Group(_ncFile, parent, name);

        makeDimensions(group, element);
        makeVariables(group, element);
        makeGroups(group, element);
        
        return group;
    }
    

    /**
     * Return the value of the "attName" XML attribute for the first variable
     * Element with the "name" XML attribute = varName.
     */
    protected String getVariableXmlAttribute(String varName, String attName) {
        String s = null;

        try {
            //Use XPath query
            Element element = getNetcdfFile().getNetcdfElement();
            Namespace ns = element.getNamespace();
            String ns_prefix = ns.getPrefix();
            if (ns_prefix.equals("")) ns_prefix = "dummy"; //XPath doesn't support a default namespace
            String ns_uri = ns.getURI();

            String q = "//" + ns_prefix + ":variable[@name=\"" + varName + "\"]";
            XPath x = XPath.newInstance(q);
            x.addNamespace(ns_prefix, ns_uri);
            Element e = (Element) x.selectSingleNode(element);
            if (e != null) s = e.getAttributeValue(attName);

        } catch (JDOMException e) {
            e.printStackTrace();
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
