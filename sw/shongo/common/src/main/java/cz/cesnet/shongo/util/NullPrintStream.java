package cz.cesnet.shongo.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * PrintStream that doesn't output anything to anywhere.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class NullPrintStream extends PrintStream
{
    /**
     * Construct null printInfo stream.
     */
    public NullPrintStream()
    {
        super(new OutputStream()
        {
            @Override
            public void write(final byte[] b) throws IOException
            {
            }

            @Override
            public void write(final byte[] b, final int off, final int len) throws IOException
            {
            }

            @Override
            public void write(final int b) throws IOException
            {
            }
        });
    }
}
