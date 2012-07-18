package cz.cesnet.shongo.controller.api;

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
     * @param resource resource; should contains all attributes marked as {@link ComplexType.Required}
     *                   in {@link ReservationRequest}
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
    public void modifyResource(SecurityToken token, Resource resource);

    /**
     * Deletes a given resource from Shongo management.
     * <p/>
     * The operation is permitted only when the user with the given token is the resource owner and the resource
     * is not user in any future reservation.
     *
     * @param token      token of the user requesting the operation
     * @param resourceId Shongo identifier of the resource to delete
     */
    @API
    public void deleteResource(SecurityToken token, String resourceId);

    /**
     * Lists all Shongo-managed resources matching the filter.
     *
     * @param token  token of the user requesting the operation
     * @return array of resource summaries
     */
    @API
    ResourceSummary[] listResources(SecurityToken token);

    /**
     * Gets the complete resource object.
     *
     * @param token      token of the user requesting the operation
     * @param resourceId Shongo identifier of the resource to get
     * @return
     */
    @API
    public Resource getResource(SecurityToken token, String resourceId);

    /**
     * Checks whether a given resource is used by any reservation in specified date/time.
     *
     * @param token      token of the user requesting the operation
     * @param resourceId Shongo identifier of the resource to check
     * @param dateTime   date/time to check
     * @return
     */
    //@API
    //boolean isResourceActive(SecurityToken token, String resourceId, AbsoluteDateTime dateTime);
}
