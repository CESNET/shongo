package cz.cesnet.shongo.controller.api.domains.response;


import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a reservation for foreign resource.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class AbstractResponse
{
    @JsonProperty("status")
    private Status status = Status.OK;

    @JsonProperty("message")
    private String message;

    public Status getStatus()
    {
        return status;
    }

    public boolean success()
    {
        return Status.OK.equals(status);
    }

    public boolean failed()
    {
        return Status.FAILED.equals(status);
    }

    public boolean error()
    {
        return Status.ERROR.equals(status);
    }

    public void setStatus(Status status)
    {
        this.status = status;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public Object toApi()
    {
        return null;
    }

    public static enum Status
    {
        OK,
        FAILED,
        ERROR;
    }
}
