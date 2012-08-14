package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.annotation.Required;
import cz.cesnet.shongo.controller.api.xmlrpc.Service;
import cz.cesnet.shongo.fault.EntityNotFoundException;
import cz.cesnet.shongo.fault.FaultException;
import org.joda.time.Interval;

import java.util.Collection;

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
     *                 in {@link ReservationRequest}
     * @return the created resource identifier
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
     * @param token              token of the user requesting the operation
     * @param resourceIdentifier Shongo identifier of the resource to delete
     */
    @API
    public void deleteResource(SecurityToken token, String resourceIdentifier) throws EntityNotFoundException;

    /**
     * Lists all Shongo-managed resources matching the filter.
     *
     * @param token token of the user requesting the operation
     * @return array of resource summaries
     */
    @API
    Collection<ResourceSummary> listResources(SecurityToken token);

    /**
     * Gets the complete resource object.
     *
     * @param token              token of the user requesting the operation
     * @param resourceIdentifier Shongo identifier of the resource to get
     * @return
     */
    @API
    public Resource getResource(SecurityToken token, String resourceIdentifier) throws EntityNotFoundException;

    /**
     * Gets the information about resource allocations.
     *
     * @param token              token of the user requesting the operation
     * @param resourceIdentifier identifier of the resource to get
     * @param interval
     * @return allocation information of resource with given {@code resourceIdentifier} for given {@code interval}
     */
    @API
    public ResourceAllocation getResourceAllocation(SecurityToken token, String resourceIdentifier, Interval interval)
            throws EntityNotFoundException;
}
