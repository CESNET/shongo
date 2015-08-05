package cz.cesnet.shongo.controller.api.domains.response;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Represents domain login response
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class DomainLogin {
    @JsonProperty("accessToken")
    private String accessToken;

    @JsonCreator
    public DomainLogin(@JsonProperty("accessToken") String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAccessToken() {
        return accessToken;
    }
}
