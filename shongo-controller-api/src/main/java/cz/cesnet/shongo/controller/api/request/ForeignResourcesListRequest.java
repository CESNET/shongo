package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.controller.ObjectPermission;

/**
 * {@link ListRequest} for foreign resources.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ForeignResourcesListRequest extends AbstractRequest
{
    private ObjectPermission permission;

    private String tagName;

    public void setPermission(ObjectPermission permission)
    {
        this.permission = permission;
    }

    public ObjectPermission getPermission()
    {
        return permission;
    }

    public String getTagName()
    {
        return tagName;
    }

    public void setTagName(String tagName)
    {
        this.tagName = tagName;
    }
}
