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

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.servlet.ServletOutputStream;

import lasp.tss.TSSException;

/**
 * The BinaryWriter abstract class provides implementation for writing binary data to
 * the Servlet response via a DataOutputStream.
 * 
 * @author Doug Lindholm
 */
public abstract class BinaryWriter extends AbstractWriter {

    /**
     * Write data via the DataOutputStream interface.
     */
    private DataOutputStream _writer; 
    
    /**
     * Return the mime type expression used in the http "Content-Type" header.
     */
    public String getContentType() { return "application/octet-stream"; }
    
    /**
     * Hook for the WriterFactory to pass in the ServletOutputStream to write to.
     */
    public void setOutput(ServletOutputStream out) {
        _writer = new DataOutputStream(out);
    }
    
  //----- Write Methods ------------------------------------------------------

    /**
     * Write data from a ByteBuffer.
     */
    protected void writeBytes(ByteBuffer bb) {
        byte[] bytes = bb.array();
        writeBytes(bytes);
    }    
    
    /**
     * Write data from an array of bytes.
     */
    protected void writeBytes(byte[] bytes) {
        try {
            _writer.write(bytes);
        } catch (IOException e) {
            String msg = "Failed to write the byte data.";
            throw new TSSException(msg, e);
        }
    }  
    
    /**
     * Write a String as an array of bytes.
     */
    protected void writeBytes(String string) {
        byte[] b = string.getBytes();
        writeBytes(b);
    }
    
    public void flush() {
        try {
            _writer.flush();
        } catch (IOException e) {
            String msg = "Failed to flush DataOutputStream.";
            throw new TSSException(msg, e);
        }
    }        
    
    public void close() {
        try {
            _writer.close();
        } catch (IOException e) {
            String msg = "Failed to close DataOutputStream.";
            throw new TSSException(msg, e);
        }
    }
}
