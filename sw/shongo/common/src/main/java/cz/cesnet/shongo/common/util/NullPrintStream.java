package cz.cesnet.shongo.common.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * PrintStream that doesn't output anything to anywhere.
 *
 * @author Martin Srom
 */
public class NullPrintStream extends PrintStream
{
    /**
     * Construct null print stream.
     */
    public NullPrintStream()
    {
        super(new OutputStream()
        {
            @Override
            public void write(int i) throws IOException
            {
            }
        });
    }
}
