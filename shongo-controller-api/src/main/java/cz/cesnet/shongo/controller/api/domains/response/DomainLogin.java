package cz.cesnet.shongo.controller.api.domains.response;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Represents domain login response
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class DomainLogin {
    @JsonProperty("accessToken")
    private String accessToken;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
