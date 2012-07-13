package cz.cesnet.shongo.connector;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * An input stream taking commands (strings) into a queue and offering them for reading.
 * <p/>
 * The read() method blocks until there is a command to read. Clearly, as a consequence, reading must occur in a
 * separate thread, or a deadlock may occur.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class CommandInputStream extends InputStream
{
    private char commandSeparator = '\n';

    private LinkedBlockingDeque<String> commands = new LinkedBlockingDeque<String>();
    private String current = null;
    private int currentPos;

    /**
     * Adds a command to the queue.
     *
     * @param cmd command to be added; empty string may be used to indicate the end of the stream
     */
    public void pushCommand(String cmd)
    {
        commands.add(cmd);
    }

    @Override
    public int read()
    {
        if (current == null) {
            try {
                current = commands.take();
                if (current.equals("")) {
                    // empty string was provided marking end of the stream
                    return -1;
                }
                currentPos = 0;
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (currentPos < current.length()) {
            return current.charAt(currentPos++);
        }
        else {
            current = null;
            return commandSeparator;
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException
    {
        if (len < 0) {
            throw new IndexOutOfBoundsException();
        }
        else if (len == 0) {
            return 0;
        }

        if (current == null && !commands.isEmpty()) {
            current = commands.pop();
            currentPos = 0;
        }

        int readLen = (current == null ? 1 : current.length() + 1); // 1 extra for command separator
        return super.read(b, off, Math.min(len, readLen));
    }

    public char getCommandSeparator()
    {
        return commandSeparator;
    }

    public void setCommandSeparator(char commandSeparator)
    {
        this.commandSeparator = commandSeparator;
    }
}
