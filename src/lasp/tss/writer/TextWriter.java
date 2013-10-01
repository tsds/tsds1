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
package lasp.tss.writer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.log4j.Logger;

import lasp.tss.TSSException;


/**
 * Write text output to a PrintWriter.
 * 
 * @author Doug Lindholm
 */
public abstract class TextWriter extends AbstractWriter {

    // Initialize a logger.
    private static final Logger _logger = Logger.getLogger(TextWriter.class);
    
    PrintWriter _writer;

    public String getContentType() { return "text/plain"; }
    
    public void setOutput(PrintWriter writer) {
        _writer = writer;
    }
    
  //----- Print Methods ------------------------------------------------------
    
    protected void print(String string) {
        _writer.print(string);
    }
    
    protected void println(String string) {
        _writer.println(string);
    }

    protected void println() {
        _writer.println();
    }
    
    /**
     * Read the contents of a file and write to the output.
     */
    protected void print(File file) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            while(true) {
                line = reader.readLine();
                if (line == null) break;
                println(line);
            }  
        } catch (Exception e) {
            String msg = "Failed to write file: " + file;
            _logger.error(msg, e);
            throw new TSSException(msg, e);
        } finally {
            try {reader.close();} catch (IOException e) {}
        }
    }
    
    
    /**
     * Utility method to read the contents of a file into a String.
     */
    public static String readFile(String file) {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            while(true) {
                line = reader.readLine();
                if (line == null) break;
                sb.append(line).append("\n");
            }  
        } catch (Exception e) {
            String msg = "Failed to read file: " + file;
            _logger.error(msg, e);
            throw new TSSException(msg, e);
        } finally {
            try {reader.close();} catch (IOException e) {}
        }
        
        return sb.toString();
    }
    
    
  //----- Format Methods -----------------------------------------------------
    
    /**
     * Format the String values with the given format string.
     */
    protected String format(String format, String... values) {
        return String.format(format, (Object[]) values);
    }
    
    /**
     * Format the double values with the given format string.
     */
    protected String format(String format, double... values) {
        int n = values.length;
        Object[] bigD = new Double[n];
        for (int i=0; i<n; i++) bigD[i] = new Double(values[i]);

        return String.format(format, bigD);
    }
    
  //-----  -----
    
    /**
     * Check if the PrintWriter has encountered an error.
     * PrintWriter does not throw IOException.
     * This causes a flush which is a performance hit. Don't call for each time sample.
     */
    public boolean checkError() { 
        boolean b = _writer.checkError();
        return b;
    }
    
    public void flush() {
        _writer.flush();
    }        
    
    public void close() {
        _writer.close();
    }
    
}
