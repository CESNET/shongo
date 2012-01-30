package cz.cesnet.shongo.measurement.common;

import java.io.*;
import java.util.ArrayList;
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
     * Add output stream
     *
     * @param outputStream
     */
    public void addOutput(OutputStream outputStream)
    {
        outputStreamList.add(outputStream);
    }

    /**
     * Connect the InputStream and OutputStream objects specified in the constructor.
     */
    public void run()
    {
        assert(inputStream != null);

        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);

        int bufferSize = 4096;
        int bufferPosition = 0;
        int bufferPositionWritten = 0;
        byte[] buffer = new byte[bufferSize];
        int bufferReadCount = 0;
        try {
            while ( (bufferReadCount = inputStream.read(buffer, bufferPosition, bufferSize - bufferPosition)) != -1 ) {
                boolean print = false;
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
        for ( OutputStream outputStream : outputStreamList ) {
            PrintStream printStream = null;
            if ( outputStream instanceof PrintStream )
                printStream = (PrintStream) outputStream;
            if ( printStream != null && name != null )
                printStream.printf("%s: ", name);
            outputStream.write(buffer, bufferPosition, bufferCount);
            outputStream.write('\n');
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