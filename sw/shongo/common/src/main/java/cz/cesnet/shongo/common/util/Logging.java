package cz.cesnet.shongo.common.util;

/**
 * Helper class for logging
 *
 * @author Martin Srom
 */
public class Logging
{
    private static boolean installed = false;

    /**
     * Install bridge to logging systems
     */
    public static synchronized void installBridge()
    {
        if (installed) {
            return;
        }
        installed = true;

        java.util.logging.LogManager.getLogManager().reset();
        org.slf4j.bridge.SLF4JBridgeHandler.install();
        java.util.logging.Logger.getLogger("global").setLevel(java.util.logging.Level.FINEST);
    }
}
