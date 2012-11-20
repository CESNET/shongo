package cz.cesnet.shongo.util;

import cz.cesnet.shongo.shell.Shell;
import org.apache.log4j.Level;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

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
     * Set filter
     *
     * @param filter
     */
    public void setFilter(String filter)
    {
        clearFilters();
        if (filter != null) {
            final String filterText = filter.toLowerCase();
            addFilter(new Filter()
            {
                @Override
                public int decide(LoggingEvent event)
                {
                    if (event.getLevel().isGreaterOrEqual(Level.WARN)) {
                        return Filter.NEUTRAL;
                    }
                    String message = event.getLoggerName() + event.getRenderedMessage();
                    message = message.toLowerCase();
                    if (message.indexOf(filterText) != -1) {
                        return Filter.NEUTRAL;
                    }
                    else {
                        return Filter.DENY;
                    }
                }
            });
        }
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
