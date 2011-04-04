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
package lasp.tss.variable;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import lasp.tss.TSSException;
import lasp.tss.TSSPublicException;
import lasp.tss.util.JulianDate;
import lasp.tss.util.RegEx;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.units.DateUnit;

/**
 * Variable to represent the time variable.
 * 
 * @author Doug Lindholm
 */
public class TimeVariable extends IndependentVariable {

    // Initialize a logger.
    private static final Logger _logger = Logger.getLogger(TimeVariable.class);
    
    /**
     * Keep a cached copy of the DateUnit so we don't have to remake it
     */
    private DateUnit _dateUnit; 
    
    /**
     * Hack to support Julian Date since the netCDF DateUnit does not support it.
     */
    private boolean _isJulianDate = false;
    
    /**
     * The format for the string representation of the time, if defined.
     * If time unit is "formatted", the data will be stored as doubles
     * in Java/Unix time: milliseconds since 1970-01-01 00:00 GMT.
     */
    private String _format;

    /**
     * Keep a collection of DateFormat-s mapped to by its String representation.
     * For performance purposes.
     */
    private HashMap<String,DateFormat> _dateFormatMap;

    
    /**
     * Construct a TimeVariable based on its nc2.Variable.
     */
    public TimeVariable(CompositeVariable parent, Variable ncVariable) {
        super(parent, ncVariable);
        
        _dateFormatMap = new HashMap<String,DateFormat>();
        parseTimeUnits();
        
        //Hack to convert formatted times to doubles and replace the internal nc Variable
        if (isFormatted()) {
            getValues();
        }
    }

    /**
     * Does this variable have a non-numeric representation.
     */
    private boolean isFormatted() {
        return (_format != null);
    }
    
    /**
     * Make sense of the time units.
     */
    private void parseTimeUnits() {
        Variable ncvar = getNetcdfVariable();
        String timeUnit = ncvar.getUnitsString(); 
        if (timeUnit == null) timeUnit = "milliseconds since 1970-01-01T00:00";  //default to Unix/Java time, 

        //Support JulianDate units which DateUnit does not support.
        if (timeUnit.toLowerCase().startsWith("julian")) {
            _isJulianDate = true;
        } else {
            //Handle formatted times (e.g. yyyy-MM-dd)
            if (timeUnit.equals("formatted")) {
                _format = ncvar.findAttribute("format").getStringValue();
                
                //change to default units, reformatting will happen during read
                timeUnit = "milliseconds since 1970-01-01";
                //change variable attributes
                Attribute att = new Attribute("units", timeUnit);
                ncvar.addAttribute(att);
                ncvar.removeAttribute("format");
            }
            
            //Manage units with NetCDF DateUnit
            try {
                _dateUnit = new DateUnit("0 "+timeUnit);
            } catch (Exception e) {
                String msg = "Failed to parse time unit: " + timeUnit;
                _logger.error(msg, e);
                throw new TSSException(msg, e);
            }
        }
    }
    
    /**
     * Return the time represented by the given string as a double.
     * May involve converting an ISO format to the defined time unit.
     */
    public double parseValue(String s) {
        double d = Double.NaN;
        
        if (s.matches(RegEx.TIME)) {
            d = convertIsoTime(s);
        } else d = Double.parseDouble(s);
        
        return d;
    }
    
    /**
     * Return the given value as a Date, making use of the units.
     */
    public Date getValueAsDate(double time) {
        Date date = null;
        
        if (_isJulianDate) {
            JulianDate jd = new JulianDate(time);
            date = jd.getDate();
        } else {
            date = _dateUnit.makeDate(time);
        }
        
        return date;
    }
    
    /**
     * Override to deal with formatted time strings.
     * Return as doubles.
     */
    public double[] getValues() {
        double[] d = null;
        
        Array array = read();
        if (array == null) return null;
        
        if (isFormatted()) {
            //parse formatted times only once
            //String format = getNetcdfVariable().findAttribute("format").getStringValue();
            DateFormat dateFormat = getDateFormat(_format);
            
            int n = (int) array.getSize();
            d = new double[n];
            
            int i = 0;
            while(array.hasNext()) {
                String s = array.next().toString(); //could be string or number
                Date date = null;
                try {
                    date = dateFormat.parse(s);
                } catch (ParseException e) {
                    String msg = "Failed to parse time unit: " + s;
                    _logger.error(msg, e);
                    throw new TSSException(msg, e);
                }
                d[i++] = date.getTime();
            }

            //Replace the internal nc Variable (containing formatted time strings) with one with numbers.
            Variable ncvar = getNetcdfVariable();
            int[] shape = ncvar.getShape();
            Array arr = Array.factory(DataType.DOUBLE, shape, d);
            Variable ncvar2 = new Variable(ncvar);
            ncvar2.setDataType(DataType.DOUBLE);
            ncvar2.setCachedData(arr, false);
            
            _format = null; //no longer formatted

        } else d = super.getValues();
        
        return d;
    }

    /**
     * Convert the ISO 8601 formatted time to a double in the predefined units.
     */
    private double convertIsoTime(String isoTime) {
        double value = Double.NaN;
        
        Date date = DateUnit.getStandardOrISO(isoTime);

        //special handling for JuianDate since DateUnit does not suport it.
        if (_isJulianDate) {
            JulianDate jd = new JulianDate(date);
            value = jd.getJulianDate();
        } else {
            value = _dateUnit.makeValue(date);
        }

        return value;
    }
    
    /**
     * Apply the format to the time value (in predefined units) and return as a String.
     */
    public String format(String format, double time) {
        Date date = getValueAsDate(time);
        
        DateFormat df = getDateFormat(format);
        String formattedTime = df.format(date);

        return formattedTime;
    }
    
    /**
     * Get the DateFormat object for the given format string.
     */
    private DateFormat getDateFormat(String format) {
        //see if we have already made one
        DateFormat dateFormat = _dateFormatMap.get(format);
        
        try {
            if (dateFormat == null) {
                dateFormat = new SimpleDateFormat(format);
                dateFormat.setTimeZone(TimeZone.getTimeZone("GMT")); //assumes GMT time zone
                _dateFormatMap.put(format, dateFormat);
            }
        } catch (Exception e) {
            String msg = "Unable to parse the time format: " + format;
            _logger.error(msg, e);
            throw new TSSPublicException(msg, e);
        }

        return dateFormat;
    }

    /**
     * Deal with time's special case of a formatted representation of time.
     * If units is "formatted" then the "format" attribute is used.
     */
    public String getUnits() {
        String units = super.getUnits();
        
        if ("formatted".equals(units)) {
            units = getAttributeValue("format");
            if (units == null) {
                String msg = "The formatted time variable does not define its format.";
                _logger.error(msg);
                throw new TSSException(msg);
            }
        }
        
        return units;
    }
}
