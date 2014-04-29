package cz.cesnet.shongo.client.web;

/**
 * Design for shongo-client-web.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Design
{
    private String resourcesFolder;

    public Design(ClientWebConfiguration configuration)
    {
        resourcesFolder = configuration.getDesignUrl();
    }

    public String getResourcesFolder()
    {
        return resourcesFolder;
    }
}
