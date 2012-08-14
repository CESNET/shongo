package cz.cesnet.shongo.controller.api;

/**
* TODO:
*
* @author Martin Srom <martin.srom@cesnet.cz>
*/
public class AllocatedResource
{
    private String resourceIdentifier;

    private String resourceName;

    public String getResourceIdentifier()
    {
        return resourceIdentifier;
    }

    public void setResourceIdentifier(String resourceIdentifier)
    {
        this.resourceIdentifier = resourceIdentifier;
    }

    public String getResourceName()
    {
        return resourceName;
    }

    public void setResourceName(String resourceName)
    {
        this.resourceName = resourceName;
    }
}
