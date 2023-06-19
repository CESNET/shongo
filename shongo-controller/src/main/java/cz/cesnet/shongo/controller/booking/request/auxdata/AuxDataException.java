package cz.cesnet.shongo.controller.booking.request.auxdata;

public class AuxDataException extends Exception
{

    public AuxDataException(String message)
    {
        super(message);
    }

    public AuxDataException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
