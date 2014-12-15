package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.api.jade.CommandException;

/**
 * An exception thrown by connector, when there is not enough free space on TCS server.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class NotEnoughSpaceException extends RecordingUnavailableException{
    public static final String CODE = "no-free-space-on-TCS";

    /**
     * Constructor.
     */
    protected NotEnoughSpaceException()
    {
    }

    /**
     * @param message description of the failure
     */
    public NotEnoughSpaceException(String message)
    {
        super(message);
    }

    /**
     * @param message description of the failure
     * @param cause   the cause of the failure
     */
    public NotEnoughSpaceException(String message, Throwable cause)
    {
        super(message, cause);
    }

    @Override
    public String getCode()
    {
        return CODE;
    }
}
