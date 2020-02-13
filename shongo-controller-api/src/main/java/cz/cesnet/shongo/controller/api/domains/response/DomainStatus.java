package cz.cesnet.shongo.controller.api.domains.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import cz.cesnet.shongo.controller.api.Domain;

/**
 * Represents domain status response
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class DomainStatus {
    @JsonProperty("status")
    private String status;

    @JsonCreator
    public DomainStatus(@JsonProperty("status") String status) {
        this.status = status;
    }

    public Domain.Status toStatus()
    {
        try {
            return Domain.Status.valueOf(status);
        }
        catch (IllegalArgumentException e) {
            return Domain.Status.NOT_AVAILABLE;
        }
    }
}
