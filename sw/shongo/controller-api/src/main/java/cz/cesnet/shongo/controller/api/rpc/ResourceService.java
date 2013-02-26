package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.api.annotation.Required;
import cz.cesnet.shongo.api.rpc.Service;
import cz.cesnet.shongo.controller.api.Resource;
import cz.cesnet.shongo.controller.api.ResourceAllocation;
import cz.cesnet.shongo.controller.api.ResourceSummary;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.fault.EntityNotFoundException;
import cz.cesnet.shongo.fault.FaultException;
import org.joda.time.Interval;

import java.util.Collection;
import java.util.Map;

/**
 * Interface to the service handling operations on resources.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public interface ResourceService extends Service
{
    /**
     * Creates a new resource that will be managed by Shongo.
     * <p/>
     * The user with the given token will be the resource owner.
     *
     * @param token    token of the user requesting the operation
     * @param resource resource; should contains all attributes marked as {@link Required}
     *                 in {@link cz.cesnet.shongo.controller.api.ReservationRequest}
     * @return the created resource shongo-id
     */
    @API
    public String createResource(SecurityToken token, Resource resource) throws FaultException;

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
    public void modifyResource(SecurityToken token, Resource resource) throws FaultException;

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
    public void deleteResource(SecurityToken token, String resourceId) throws FaultException;

    /**
     * Lists all Shongo-managed resources matching the filter.
     *
     * @param token  token of the user requesting the operation
     * @param filter attributes for filtering resources (map of name => value pairs)::
     *               -{@code userId}  restricts resource owner by his user-id
     * @return array of resource summaries
     */
    @API
    Collection<ResourceSummary> listResources(SecurityToken token, Map<String, Object> filter);

    /**
     * Gets the complete resource object.
     *
     * @param token      token of the user requesting the operation
     * @param resourceId shongo-id of the resource to get
     * @return the complete resource object
     */
    @API
    public Resource getResource(SecurityToken token, String resourceId) throws EntityNotFoundException;

    /**
     * Gets the information about resource allocations.
     *
     * @param token      token of the user requesting the operation
     * @param resourceId shongo-id of the resource to get
     * @param interval
     * @return allocation information of resource with given {@code resourceId} for given {@code interval}
     */
    @API
    public ResourceAllocation getResourceAllocation(SecurityToken token, String resourceId, Interval interval)
            throws EntityNotFoundException;
}
