package cz.cesnet.shongo.controller.api.domains;

import cz.cesnet.shongo.controller.api.Domain;

/**
 * InterDomain actions
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class InterDomainAction {
    public final static String DOMAIN_LOGIN = "/domain/login";
    public final static String DOMAIN_STATUS = "/domain/status";
    public final static String DOMAIN_RESOURCES_LIST = "/domain/resource/list";
    public final static String DOMAIN_CAPABILITY_LIST = "/domain/capability/list";
    public final static String DOMAIN_ALLOCATE = "/domain/allocate";
    public final static String DOMAIN_RESERVATION_DATA = "/domain/reservation/data";
    public final static String DOMAIN_RESERVATION_LIST = "/domain/reservation/list";
    public final static String DOMAIN_RESERVATION_REQUEST_DELETE = "/domain/reservation_request/delete";

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
