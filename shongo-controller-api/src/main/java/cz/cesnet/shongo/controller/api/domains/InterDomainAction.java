package cz.cesnet.shongo.controller.api.domains;

import cz.cesnet.shongo.controller.api.Domain;

/**
 * InterDomain actions
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class InterDomainAction {
    public final static String DOMAIN_STATUS = "/domain/status";
    public final static String DOMAIN_RESOURCES_LIST = "/domain/resource/list";

    public enum HttpMethod {
        GET("GET"), POST("POST"), PUT("PUT"), DELETE("DELETE");

        private final String value;

        HttpMethod(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
