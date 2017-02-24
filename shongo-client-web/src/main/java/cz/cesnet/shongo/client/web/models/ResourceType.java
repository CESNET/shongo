package cz.cesnet.shongo.client.web.models;

import java.util.ResourceBundle;

/**
 * Created by Marek Perichta.
 */
public enum ResourceType {

    /**
     * General resource.
     */
    RESOURCE("views.resource.resource"),

    /**
     * Indicates video-conference resource.
     */
    DEVICE_RESOURCE("views.resource.deviceResource");

    private final String code;

    ResourceType (String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

}
