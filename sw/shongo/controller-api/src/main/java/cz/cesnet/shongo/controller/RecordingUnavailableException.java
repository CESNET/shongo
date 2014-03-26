package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.api.jade.CommandException;

/**
 * An exception thrown by connector, when recording is not available at the moment.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class RecordingUnavailableException extends CommandException
{
    public static final String CODE = "recording-unavailable";

    /**
     * Constructor.
     */
    protected RecordingUnavailableException()
    {
    }

    /**
     * @param message description of the failure
     */
    public RecordingUnavailableException(String message)
    {
        super(message);
    }

    /**
     * @param message description of the failure
     * @param cause   the cause of the failure
     */
    public RecordingUnavailableException(String message, Throwable cause)
    {
        super(message, cause);
    }

    @Override
    public String getCode()
    {
        return CODE;
    }
}
