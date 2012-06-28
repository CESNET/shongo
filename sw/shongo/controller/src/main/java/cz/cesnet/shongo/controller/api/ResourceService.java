package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.common.api.Service;

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
     * @param token      token of the user requesting the operation
     * @param domain     identifier of the domain to create the resource in
     * @param attributes map of resource attributes; should only contain attributes specified in the Resource class
     *                   while all the attributes marked as required must be present
     * @return the created resource identifier
     */
    public String createResource(API.SecurityToken token, String domain, Map attributes);

    /**
     * Modifies a given resource.
     * <p/>
     * The operation is permitted only when the user with the given token is the resource owner and only when
     * the modification does not cancel any existing reservations (the existing ones might be rescheduled, though).
     *
     * @param token      token of the user requesting the operation
     * @param resourceId Shongo identifier of the resource to modify
     * @param attributes map of resource attributes to change;
     */
    public void modifyResource(API.SecurityToken token, String resourceId, Map attributes);

    /**
     * Deletes a given resource from Shongo management.
     * <p/>
     * The operation is permitted only when the user with the given token is the resource owner and the resource
     * is not user in any future reservation.
     *
     * @param token      token of the user requesting the operation
     * @param resourceId Shongo identifier of the resource to delete
     */
    public void deleteResource(API.SecurityToken token, String resourceId);

    /**
     * Gets the complete resource object.
     *
     * @param token      token of the user requesting the operation
     * @param resourceId Shongo identifier of the resource to get
     * @return
     */
    public Resource getResource(API.SecurityToken token, String resourceId);

    /**
     * Lists all Shongo-managed resources matching the filter.
     *
     * @param token  token of the user requesting the operation
     * @param filter
     * @return
     */
    ResourceSummary[] listResources(API.SecurityToken token, Map filter);

    /**
     * Checks whether a given resource is used by any reservation in specified date/time.
     *
     * @param token      token of the user requesting the operation
     * @param resourceId Shongo identifier of the resource to check
     * @param dateTime   date/time to check
     * @return
     */
    boolean isResourceActive(API.SecurityToken token, String resourceId, API.AbsoluteDateTime dateTime);
}
