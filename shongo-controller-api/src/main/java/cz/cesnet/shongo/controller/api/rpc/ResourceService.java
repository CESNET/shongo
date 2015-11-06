package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.api.rpc.Service;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.domains.response.DomainCapability;
import cz.cesnet.shongo.controller.api.request.*;
import org.joda.time.Interval;

import java.util.List;
import java.util.Set;

/**
 * Interface to the service handling operations on resources.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public interface ResourceService extends Service {
    /**
     * Creates a new resource that will be managed by Shongo.
     * <p/>
     * The user with the given token will be the resource owner.
     *
     * @param token    token of the user requesting the operation
     * @param resource resource; should contains all required attributes
     *                 in {@link cz.cesnet.shongo.controller.api.ReservationRequest}
     * @return the created resource shongo-id
     */
    @API
    public String createResource(SecurityToken token, Resource resource);

    /**
     * Modifies a given resource.
     * <p/>
     * The operation is permitted only when the user with the given token is the resource owner and only when
     * the modification does not cancel any existing reservations (the existing ones might be rescheduled, though).
     *
     * @param token    token of the user requesting the operation
     * @param resource resource with attributes to be modified
     */
    @API
    public void modifyResource(SecurityToken token, Resource resource);

    /**
     * Deletes a given resource from Shongo management.
     * <p/>
     * The operation is permitted only when the user with the given token is the resource owner and the resource
     * is not user in any future reservation.
     *
     * @param token      token of the user requesting the operation
     * @param resourceId shongo-id of the resource to delete
     */
    @API
    public void deleteResource(SecurityToken token, String resourceId);

    /**
     * Lists all Shongo-managed resources matching the filter.
     *
     * @param request
     * @return collection of resource summaries
     */
    @API
    public ListResponse<ResourceSummary> listResources(ResourceListRequest request);

    /**
     * Gets the complete resource object.
     *
     * @param token      token of the user requesting the operation
     * @param resourceId shongo-id of the resource to get
     * @return the complete resource object
     */
    @API
    public Resource getResource(SecurityToken token, String resourceId);

    /**
     * Gets the information about resource allocations.
     *
     * @param token      token of the user requesting the operation
     * @param resourceId shongo-id of the resource to get
     * @param interval
     * @return allocation information of resource with given {@code resourceId} for given {@code interval}
     */
    @API
    public ResourceAllocation getResourceAllocation(SecurityToken token, String resourceId, Interval interval);

    @API
    public String createTag(SecurityToken token, Tag tag);

    @API
    public List<Tag> listTags(TagListRequest request);

    @API
    public Tag getTag(SecurityToken token, String tagId);

    @API
    public Tag findTag(SecurityToken token, String name);

    @API
    public void modifyTag(SecurityToken token, Tag tag);

    @API
    public void deleteTag(SecurityToken token, String tagId);

    @API
    public void assignResourceTag(SecurityToken token, String resourceId, String tagId);

    @API
    public void removeResourceTag(SecurityToken token, String resourceId, String tagId);

    @API
    public String createDomain(SecurityToken token, Domain domain);

//    @API
//    public List<DomainCapability> listDomainCapabilities(DomainCapabilityListRequest request);

    @API
    public Domain getDomain(SecurityToken token, String domainId);

    @API
    public String getDomainName(SecurityToken token, String domainId);

    @API
    public void modifyDomain(SecurityToken token, Domain domain);

    @API
    public void deleteDomain(SecurityToken token, String domainId);

//    @API
//    TODO:public void addDomainTag(SecurityToken token, DomainTag domainTag);
//
//    @API
//    TODO:public void removeDomainTag(SecurityToken token, String domainId, String tagId);

    @API
    public void addDomainResource(SecurityToken token, DomainResource domainResource, String domainId, String resourceId);

    @API
    public void removeDomainResource(SecurityToken token, String domainId, String resourceId);

    /**
     * TODO:MR Create ACL for public access
     * Just temporarily without {@link cz.cesnet.shongo.controller.api.SecurityToken}
     *
     * Returns resources IDs with public calendar.
     *
     * @return list of resources with public calendar access
     */
    @API
    public ListResponse<ResourceSummary> getResourceIdsWithPublicCalendar();

    @API
    public String getLocalDomainPasswordHash(SecurityToken token);

    @API
    public ListResponse<ResourceSummary> listForeignResources(ForeignResourcesListRequest request);
}
