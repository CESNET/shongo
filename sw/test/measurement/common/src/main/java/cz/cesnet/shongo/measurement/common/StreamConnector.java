package cz.cesnet.shongo.measurement.common;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A class for connecting an OutputStream to an InputStream.
 *
 * @author Martin Srom
 */
public class StreamConnector extends Thread
{
    /**
     * Input stream to read from
     */
    private InputStream inputStream = null;

    /**
     * Output stream to write to
     */
    private List<OutputStream> outputStreamList = new ArrayList<OutputStream>();

    /**
     * Name to append
     */
    private String name;

    /**
     * Flag if end of stream should be also forwarded
     */
    private boolean forwardStreamEnd = false;

    /**
     * Stream listener
     */
    public static interface Listener
    {
        public boolean onRead(String buffer);
    }

    /** Listeners */
    private List<Listener> listeners = new ArrayList<Listener>();

    /**
     * Specify the streams that this object will connect in the run()
     * method.
     *
     * @param inputStream the InputStream to read from.
     */
    public StreamConnector(InputStream inputStream)
    {
        this.inputStream = inputStream;
    }

    /**
     * Specify the streams that this object will connect in the run()
     * method.
     *
     * @param inputStream the InputStream to read from.
     * @param outputStream the OutputStream to write to.
     */
    public StreamConnector(InputStream inputStream, OutputStream outputStream)
    {
        this.inputStream = inputStream;
        this.outputStreamList.add(outputStream);
    }

    /**
     * Specify the streams that this object will connect in the run()
     * method.
     *
     * @param inputStream the InputStream to read from.
     * @param outputStream the OutputStream to write to.
     */
    public StreamConnector(InputStream inputStream, OutputStream outputStream, String name)
    {
        this.inputStream = inputStream;
        this.outputStreamList.add(outputStream);
        this.name = name;
    }

    /**
     * Set flag if end of stream should be also forwarded
     *
     * @param forwardStreamEnd
     */
    public void setForwardStreamEnd(boolean forwardStreamEnd)
    {
        this.forwardStreamEnd = forwardStreamEnd;
    }

    /**
     * Add output stream
     *
     * @param outputStream
     */
    public void addOutput(OutputStream outputStream)
    {
        outputStreamList.add(outputStream);
    }

    /**
     * Add listener
     *
     * @param listener
     */
    public void addListener(Listener listener)
    {
        listeners.add(listener);
    }

    /**
     * Read the InputStream and write the data to OutputStream instances.
     */
    public void run()
    {
        assert(inputStream != null);

        int bufferSize = 4096;
        int bufferPosition = 0;
        int bufferPositionWritten = 0;
        byte[] buffer = new byte[bufferSize];
        int bufferReadCount = 0;
        try {
            while ( (bufferReadCount = inputStream.read(buffer, bufferPosition, bufferSize - bufferPosition)) != -1 ) {
                for ( int index = 0; index < bufferReadCount; index++ ) {
                    char currentChar = (char)buffer[bufferPosition + index];
                    if ( currentChar == '\n' || currentChar == '\r' ) {
                        int count = bufferPosition + index - bufferPositionWritten + 1;
                        printOutput(buffer, bufferPositionWritten, count - 1);
                        bufferPositionWritten += count;
                    }
                }
                bufferPosition += bufferReadCount;
                if ( bufferPosition == bufferPositionWritten ) {
                    bufferPosition = 0;
                    bufferPositionWritten = 0;
                }
            }
            // forward also the end of the stream
            if ( forwardStreamEnd ) {
                for (OutputStream outputStream : outputStreamList) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Print to output streams
     *
     * @param buffer
     * @param bufferPosition
     * @param bufferCount
     * @throws IOException
     */
    private void printOutput(byte[] buffer, int bufferPosition, int bufferCount) throws IOException
    {
        if ( listeners.size() > 0 ) {
            String text = new String(buffer, bufferPosition, bufferCount);
            for ( Listener listener : listeners ) {
                if ( listener.onRead(text.toString()) == false )
                    return;
            }    
        }
        
        for ( OutputStream outputStream : outputStreamList ) {
            // prepare the byte array to output atomically to prevent mixing of multiple streams together
            boolean printName = (outputStream instanceof PrintStream && name != null);
            byte[] outBuf = new byte[ (printName ? name.getBytes().length + 2 : 0) + bufferCount + 1 ];
            int i = 0;
            if (printName) {
                byte[] nameBytes = name.getBytes();
                for ( ; i < nameBytes.length; i++) {
                    outBuf[i] = nameBytes[i];
                }
                outBuf[i++] = ':';
                outBuf[i++] = ' ';
            }
            
            for (int j = 0; j < bufferCount; j++) {
                outBuf[i++] = buffer[bufferPosition + j];
            }
            outBuf[i++] = '\n';
            outputStream.write(outBuf);
            outputStream.flush();
        }
    }

    /**
     * Start thread
     */
    @Override
    public void start()
    {
        if ( isAlive() )
            return;
        try{
            super.start();
        } catch ( IllegalThreadStateException e) {
        }
    }
}