package cz.cesnet.shongo.util;

import java.io.PrintStream;

/**
 * Helper class for logging.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Logging
{
    /**
     * Flag if logging bridge was installed.
     */
    private static boolean bridgeInstalled = false;

    /**
     * Install bridge from other logging systems to SLF4J.
     * For example java.util.logging needs this bridge.
     */
    public static synchronized void installBridge()
    {
        if (bridgeInstalled) {
            return;
        }
        bridgeInstalled = true;

        java.util.logging.LogManager.getLogManager().reset();
        org.slf4j.bridge.SLF4JBridgeHandler.install();
        java.util.logging.Logger.getLogger("global").setLevel(java.util.logging.Level.FINEST);
    }

    /**
     * Saved System.out property.
     */
    private static PrintStream systemOut;

    /**
     * Disable printing by System.out.
     */
    public static synchronized void disableSystemOut()
    {
        systemOut = System.out;
        System.setOut(new NullPrintStream());
    }

    /**
     * Enable printing by System.out.
     */
    public static synchronized void enableSystemOut()
    {
        if (systemOut != null) {
            System.setOut(systemOut);
            systemOut = null;
        }
    }

    /**
     * Saved System.err property.
     */
    private static PrintStream systemErr;

    /**
     * Disable printing by System.err.
     */
    public static synchronized void disableSystemErr()
    {
        systemErr = System.err;
        System.setErr(new NullPrintStream());
    }

    /**
     * Enable printing by System.err.
     */
    public static synchronized void enableSystemErr()
    {
        if (systemErr != null) {
            System.setErr(systemErr);
            systemErr = null;
        }
    }
}
