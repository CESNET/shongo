package cz.cesnet.shongo.common.util;

import cz.cesnet.shongo.common.shell.Shell;

import java.io.IOException;
import java.io.Writer;

/**
 * Console appender that knows about active Shell, and when
 * is writing to the console it rePrompt the shell.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ConsoleAppender extends org.apache.log4j.ConsoleAppender
{
    /**
     * Construct appender.
     */
    public ConsoleAppender()
    {
    }

    /**
     * Set writer to appender.
     *
     * @param writer
     */
    @Override
    public void setWriter(Writer writer)
    {
        super.setWriter(new ShellAwareWriter(writer));
    }

    /**
     * Writer that know about active Shell
     *
     * @author Martin Srom <martin.srom@cesnet.cz>
     */
    private static class ShellAwareWriter extends Writer
    {
        private Writer writer;
        private boolean active;

        public ShellAwareWriter(Writer writer)
        {
            this.writer = writer;
        }

        @Override
        public void write(char[] chars, int i, int i1) throws IOException
        {
            Shell activeShell = Shell.getActive();
            if (activeShell != null) {
                activeShell.rePrompt();
            }
            writer.write(chars, i, i1);
        }

        @Override
        public void flush() throws IOException
        {
            writer.flush();
        }

        @Override
        public void close() throws IOException
        {
            writer.close();
        }
    }
}
