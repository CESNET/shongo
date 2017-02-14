package cz.cesnet.shongo.client.web.models;

/**
 * Created by Marek Perichta.
 */
public enum ResourceType {

    /**
     * General resource.
     */
    RESOURCE("Resource"),

    /**
     * Indicates video-conference resource.
     */
    DEVICE_RESOURCE("Device resource");

    private final String title;

    ResourceType (String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
