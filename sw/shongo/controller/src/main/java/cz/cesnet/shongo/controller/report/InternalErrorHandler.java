package cz.cesnet.shongo.controller.report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for internal errors.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class InternalErrorHandler
{
    private static Logger logger = LoggerFactory.getLogger(InternalErrorHandler.class);

    /**
     * Handle internal error.
     *
     * @param type
     * @param message
     * @param exception
     */
    public static void handle(InternalErrorType type, String message, Exception exception)
    {
        StringBuilder messageBuilder = new StringBuilder();
        if (type != null) {
            messageBuilder.append(type.getName());
        }
        if (message != null) {
            if (messageBuilder.length() > 0) {
                messageBuilder.append(": ");
            }
            messageBuilder.append(message);
        }
        if (messageBuilder.length() == 0) {
            messageBuilder.append("Unknown");
        }

        logger.error(messageBuilder.toString(), exception);
    }

    /**
     * Handle internal error.
     *
     * @param type
     * @param exception
     */
    public static void handle(InternalErrorType type, Exception exception)
    {
        handle(type, null, exception);
    }
}
