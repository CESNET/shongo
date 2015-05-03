package cz.cesnet.shongo.controller.api.domains.response;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Represents domain status
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class DomainStatus {
    String status;

    public DomainStatus(String status) {
        this.status = status;
    }

    @JsonProperty("status")
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
