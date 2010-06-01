/*
 * Copyright (c) 2010, Regents of the University of Colorado
 * 
 * All rights reserved. Redistribution and use in source and binary forms, 
 * with or without modification, are permitted provided that the following 
 * conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer. 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution. 
 * Neither the name of the University of Colorado nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission. 
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package lasp.tss.iosp;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import lasp.tss.TSSException;

import org.apache.commons.math.util.ResizableDoubleArray;
import org.apache.log4j.Logger;
import org.jdom.Element;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Variable;

/**
 * IOSP to read data from a relational database table.
 * Supports scalar data only, for now.
 * 
 * @author Doug Lindholm
 */
public class DatabaseIOSP extends AbstractIOSP {

    // Initialize a logger.
    private static final Logger _logger = Logger.getLogger(DatabaseIOSP.class);
    
    /**
     * Data structure to contain array of doubles by variable name.
     */
    private LinkedHashMap<String,double[]> _dataMap;
    
    /**
     * Data structure to contain array of Strings by variable name.
     */
    private LinkedHashMap<String,String[]> _textDataMap;

    /**
     * Database connection.
     */
    private Connection _connection;


    /**
     * Get the number of time samples. If not yet defined, read the time samples.
     */
    protected int getLength() {
        int length = super.getLength(); //see if it is defined in the ncml
        if (length < 0) {
            double[] t = getTimes();
            length = t.length;
        }
        return length;
    }

    /**
     * Return the time values as doubles in the defined units
     * or milliseconds since 1970 (Unix time) if formatted.
     */
    protected double[] getTimes() {
        String vname = getTimeVarName(); //original time variable name
        double[] values = getValues(vname);
        return values;
    }

    /**
     * Return all the data values for the given named variable.
     * NOTE: This will read all the data for all variables
     * if it hasn't already been read.
     */
    protected double[] getValues(String varName) {
        if (_dataMap == null) readAllData();
        double[] values = _dataMap.get(varName);
        return values;
    }
    
    /**
     * Read all String values from a String variable.
     */
    protected String[] getStringValues(String varName) {
        if (_textDataMap == null) readAllData();
        String[] values = _textDataMap.get(varName);
        return values;
    }
    
    /**
     * Read all the data for all the variable for all time samples
     * via a single sql query.
     */
    protected void readAllData() {
        
        String query = buildDataQuery();
        
        try {
            ResultSet rs = executeQuery(query);
            ResultSetMetaData md = rs.getMetaData();
            
            //Construct the internal data containers
            _dataMap = new LinkedHashMap<String,double[]>();
            _textDataMap = new LinkedHashMap<String,String[]>();
            
            //Make containers to collect data as we get it from the ResultSet
            int ncol = md.getColumnCount();
            ResizableDoubleArray[] dataArrays = new ResizableDoubleArray[ncol]; //tmp holder for numeric data
            List<String>[] textDataLists = new List[ncol]; //tmp holder for text data
            for (int i=0; i<ncol; i++) {
                dataArrays[i] = new ResizableDoubleArray();
                textDataLists[i] = new ArrayList<String>();
            }

            //Read data from the result set into the ResizableDoubleArray-s which are already in the _dataArrayMap.
            while (rs.next()) { 
                for (int i=0; i<ncol; i++) {
                    double d = Double.NaN;
                    //Check type and read as appropriate
                    int typeID  = md.getColumnType(i+1); 
                    
                    if (typeID == java.sql.Types.TIMESTAMP) { //TODO: test for DATE and TIME types?
                        //We need to convert timestamps to numerical values.
                        Timestamp ts = rs.getTimestamp(i+1);
                        d = ts.getTime();
                    } else if (typeID == java.sql.Types.VARCHAR) {
                        //Text column. Save strings apart from other data.
                        //They will appear as NaNs in the numeric data values.
                        String s = rs.getString(i+1);
                        textDataLists[i].add(s);
                    } else d = rs.getDouble(i+1);
                    
                    dataArrays[i].addElement(d);
                }
            }
            
            //Extract the primitive arrays from the ResizableDoubleArray-s and put in data map.
            //Segregate the text data. Don't bother to save "data" (NaNs) for String types.
            for (int i=0; i<ncol; i++) {
                String name = md.getColumnName(i+1);
                int typeID  = md.getColumnType(i+1);
                if (typeID == java.sql.Types.VARCHAR) { 
                    String[] text = new String[1];
                    text = (String[]) textDataLists[i].toArray(text);
                    _textDataMap.put(name, text);
                } else {
                    double[] data = dataArrays[i].getElements();
                    _dataMap.put(name, data);
                }
            }
            
        } catch (SQLException e) {
            String msg = "Unable to process database query: " + query;
            _logger.error(msg, e);
            throw new TSSException(msg, e);
        } 
        
    }

    /**
     * Create the SQL query string to get the data from the database.
     */
    protected String buildDataQuery() {
        StringBuilder sql = new StringBuilder("select");
        String delim = " ";
        for (String vname : getColumnNames()) {
            sql.append(delim).append(vname);
            delim = ", ";
        }
        
        Element ncElement = getNetcdfElement();
        
        String table = ncElement.getAttributeValue("dbTable");
        if (table == null) {
            String msg = "No database table defined. Must set 'dbTable' attribute.";
            _logger.error(msg);
            throw new TSSException(msg);
        }
        sql.append(" from " + table);
        
        String predicate = ncElement.getAttributeValue("predicate");
        if (predicate != null) sql.append(" where " + predicate);
        
        //Order by time.
        String tname = getTimeVarName();
        sql.append(" order by "+tname+" ASC");
        
        return sql.toString();
    }

    /**
     * Return the database table column names to use for the sql query.
     * Same as variable names, i.e. every variable in the ncml maps to a column in the database table.
     */
    protected ArrayList<String> getColumnNames() {
        return getVariableNames();
    }
    
    /**
     * Get names of variables in the ncml. Use orgName if present. Maintains document order.
     */
    protected ArrayList<String> getVariableNames() {
        ArrayList<String> varNames = new ArrayList<String>();
        
        Element ncElement = getNetcdfElement();
        List<Element> vars = ncElement.getChildren("variable", ncElement.getNamespace()); 
        for (Element element : vars) {
            String name = getVariableName(element);
            varNames.add(name);
        }

        return varNames;
    }

    /**
     * Return the database connection. 
     * Each dataset request will get its own Connection.
     */
    private Connection getConnection() {
        if (_connection != null) return _connection;

        Element ncElement = getNetcdfElement();
        String connectionString = ncElement.getAttributeValue("connectionString");
        String dbUser = ncElement.getAttributeValue("dbUser");
        String dbPassword = ncElement.getAttributeValue("dbPassword");
        String jdbcDriver = ncElement.getAttributeValue("jdbcDriver");
        
        try {
            Class.forName(jdbcDriver);
            _connection = DriverManager.getConnection(connectionString, dbUser, dbPassword);
        } catch (Exception e) {
            String msg = "Failed to get database connection: " + connectionString;
            _logger.error(msg, e);
            throw new TSSException(msg, e);
        } 
    
        return _connection;
    }
        
    /**
     * Execute the sql select and return the ResultSet.
     */
    protected ResultSet executeQuery(String query) {
        ResultSet rs = null;
        
        try {
            Statement stmt = getConnection().createStatement();
            rs = stmt.executeQuery(query);
        } catch (SQLException e) {
            String msg = "Failed to execute query: " + query;
            _logger.error(msg, e);
            throw new TSSException(msg, e);
        } 
       
        return rs;
    }


    /**
     * This is what NetCDF will call. Make an Array of data of that variable for the given section.
     */
    public Array readData(Variable variable, Section section) throws IOException, InvalidRangeException {
        Array array = null;
        
        //Get info on time selection (1st dimension).
        int[] shape = section.getShape();
        int ntim = shape[0]; 
        int offset = section.getOrigin(0);
        int stride = section.getStride(0); 
        int length = ntim; //size of data array subset
        
        //Handle 2D section, if there is one. e.g. 2nd dim = Sequence domain
        int n2 = 1;
        int o2 = 0;
        int s2 = 1;
        int n2orig = 1; //size of original 2nd dimension (i.e. number of wavelengths) in the backing array
        if (shape.length == 2) {
            n2 = shape[1];
            o2 = section.getOrigin(1);
            s2 = section.getStride(1);
            n2orig = variable.getShape(1);
            length *= n2;
        }

        //make data subset
        String vname = variable.getName();
        DataType type = variable.getDataType();
        
        double[] d = null;
        String[] s = null;
        double[] data = null; 
        String[] sdata = null;
        if (type.isString()) {
            s = getStringValues(vname);
            sdata = new String[length];
        } else {
            d = getValues(vname); //original complete set of values
            data = new double[length];
        }

        int j=0;
        for (int itim=0; itim<ntim; itim++) {
            for (int i2=0; i2<n2; i2++) {
                //calculate index into backing array
                int index = offset*n2orig + itim*n2orig*stride + o2 + i2*s2;
                if (sdata != null) sdata[j++] = s[index]; //String
                else data[j++] = d[index];
            }
        }

        //Construct the Array.
        if (type.isString()) array = Array.factory(type, shape, sdata);
        else array = Array.factory(type, shape, data);
        
        return array;
    }


    /**
     * Close the database connection if NetCDF shuts us down.
     */
    public void close() throws IOException {
        try {
            _connection.close();
        } catch (SQLException e) {
            String msg = "Failed to close database connection.";
            _logger.warn(msg, e);
        }
        super.close();  //close the /dev/null that ncml gave us
    }
    
    public String getFileTypeDescription() {
        String s = "Relational database";
        return s;
    }

    public String getFileTypeId() {
        return "TSDS-Database";
    }


}
