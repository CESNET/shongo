package cz.cesnet.shongo.connector;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class CommandResultStream extends OutputStream
{
    /**
     * An array of strings which are to be recognized as markers of end of a command output.
     */
    private String[] endMarkers;

    private StringBuilder buffer = new StringBuilder();
    private BlockingQueue<String> outputQueue = new LinkedBlockingQueue<String>();

    public CommandResultStream(String endMarker)
    {
        endMarkers = new String[]{endMarker};
    }

    public CommandResultStream(String[] endMarkers)
    {
        this.endMarkers = endMarkers;
    }

    @Override
    public void write(int b) throws IOException
    {
        buffer.append((char) b);
        checkCompleteOutput();
    }

    @Override
    public void write(byte[] b, int off, int len)
    {
        for (int i = 0; i < len; i++) {
            buffer.append((char) b[off + i]);
        }
        checkCompleteOutput();
    }

    /**
     * Checks whether the buffer already contains a complete command output, and if so, removes it from the buffer
     * and stores it in the output queue.
     *
     * NOTE: might be optimized using the Knuth-Morris-Pratt algorithm
     */
    private void checkCompleteOutput()
    {
        int minEnd = Integer.MAX_VALUE;
        for (String em : endMarkers) {
            int pos = buffer.indexOf(em);
            if (pos != -1) {
                minEnd = Math.min(minEnd, pos + em.length());
            }
        }
        if (minEnd != Integer.MAX_VALUE) {
            outputQueue.add(buffer.substring(0, minEnd));
            buffer.delete(0, minEnd);
            checkCompleteOutput();
        }
    }

    /**
     * Retrieves the output of the least recent unhandled command. Blocks until it becomes available.
     * @return output of the least recent unhandled command
     * @throws InterruptedException
     */
    public String getOutput() throws InterruptedException
    {
        return outputQueue.take();
    }
}
