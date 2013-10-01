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
package lasp.tss.util;

import java.util.Date;

/**
 * Represent an instant in time as a Julian Date.
 * Note, this implementation does not support dates
 * before the adoption of the Gregorian Calendar (15 October 1582).
 * Since it is a UTC time scale, as are the other forms supported here,
 * there is no need to deal with leap seconds.
 * 
 * @author Doug Lindholm
 */
public class JulianDate {
    
    public static final double JULIAN_DATE_AT_UNIX_EPOCH = 2440587.5;
    
    //private double _julianDate;
    private Date _julianDate;
    
    /**
     * JulianDate at time of construction.
     */
    public JulianDate() {
        _julianDate = new Date();
    }
    
    /**
     * JulianDate for the given Date.
     */
    public JulianDate(Date date) {
        _julianDate = date;
    }
    
    /**
     * JulianDate for the given Unix time (milliseconds since 1970).
     */
    public JulianDate(long millisSinceUnixEpoch) {
        _julianDate = new Date(millisSinceUnixEpoch);
    }
    
    /**
     * Construct JulianDate from double representation.
     */
    public JulianDate(double julianDate) {
        double days = julianDate - JULIAN_DATE_AT_UNIX_EPOCH;
        long ms = (long) (days * 86400000);
        _julianDate = new Date(ms);
    }
    
    /**
     * Return this instant in Unix time - milliseconds since the Unix epoch.
     * Same as Java's internal time.
     */
    public long getMillisSinceUnixEpoch() {
        return _julianDate.getTime();
    }
    
    /**
     * Return in Julian date units.
     */
    public double getJulianDate() {
        double unixMillis = getMillisSinceUnixEpoch();
        double ms_per_day = 86400000.0;
        double jd = unixMillis / ms_per_day + JULIAN_DATE_AT_UNIX_EPOCH;
        return jd;
    }
    
    /**
     * Return Julian Day Number (fraction of day truncated).
     */
    public int getJulianDayNumber() {
        double jd = getJulianDate();
        int jdn = (int) Math.floor(jd);
        return jdn;
    }
    
    /**
     * Return Modified Julian Date
     */
    public double getModifiedJulianDate() {
        double jd = getJulianDate();
        double mjd = jd - 2400000.5;
        return mjd;
    }
    
    /**
     * Return as Java Date.
     */
    public Date getDate() {
        return _julianDate;
    }
}