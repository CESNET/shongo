package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.api.jade.CommandException;

/**
 * An exception thrown by controller, when room doesn't exists.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class RoomNotExistsException extends CommandException
{
    public static final String CODE = "room-not-exists";

    /**
     * @param roomId
     * @param deviceResourceId
     */
    public RoomNotExistsException(String roomId, String deviceResourceId)
    {
        super("Room " + roomId + " doesn't exist in device " + deviceResourceId + ".");
    }

    /**
     * @param message description of the failure
     * @param cause   the cause of the failure
     */
    public RoomNotExistsException(String message, Throwable cause)
    {
        super(message, cause);
    }

    @Override
    public String getCode()
    {
        return CODE;
    }
}
