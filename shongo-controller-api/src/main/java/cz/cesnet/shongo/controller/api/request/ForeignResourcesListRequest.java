package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.controller.ObjectPermission;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * {@link ListRequest} for foreign resources.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class ForeignResourcesListRequest extends AbstractRequest
{
    private ObjectPermission permission;

    private String tagName;

    private Boolean onlyAllocatable;

    /**
     * For filtering resources by ids.
     */
    private Set<String> resourceIds = new HashSet<>();

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

    public Set<String> getResourceIds()
    {
        return resourceIds;
    }

    public void setResourceIds(Set<String> resourceIds)
    {
        this.resourceIds = resourceIds;
    }

    public void addResourceId(String resourceId)
    {
        this.resourceIds.add(resourceId);
    }

    public Boolean getOnlyAllocatable()
    {
        return onlyAllocatable;
    }

    public void setOnlyAllocatable(Boolean onlyAllocatable)
    {
        this.onlyAllocatable = onlyAllocatable;
    }
}
