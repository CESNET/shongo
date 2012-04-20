package cz.cesnet.shongo.common.util;

import java.io.PrintStream;

/**
 * Helper class for logging
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Logging
{
    /**
     * Flag if logging bridge was installed
     */
    private static boolean bridgeInstalled = false;

    /**
     * Install bridge to logging systems
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
     * Saved System.out property
     */
    private static PrintStream systemOut;

    /**
     * Disable System.out
     */
    public static void disableSystemOut()
    {
        systemOut = System.out;
        System.setOut(new NullPrintStream());
    }

    /**
     * Enable System.out
     */
    public static void enableSystemOut()
    {
        if ( systemOut != null ) {
            System.setOut(systemOut);
            systemOut = null;
        }
    }
}
