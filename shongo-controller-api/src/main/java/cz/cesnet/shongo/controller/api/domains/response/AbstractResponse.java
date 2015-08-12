package cz.cesnet.shongo.controller.api.domains.response;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Represents a reservation for foreign resource.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public abstract class AbstractResponse
{
    @JsonProperty("status")
    private Status status = Status.OK;

    @JsonProperty("message")
    private String message;

    public Status getStatus()
    {
        return status;
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

    public static enum Status
    {
        OK,
        ERROR;
    }
}
