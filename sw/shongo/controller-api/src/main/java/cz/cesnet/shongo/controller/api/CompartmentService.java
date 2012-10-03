package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.xmlrpc.Service;
import cz.cesnet.shongo.fault.FaultException;

import java.util.Collection;

/**
 * Interface to the service handling operations on compartments.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface CompartmentService extends Service
{
    /**
     * Deletes a given compartment.
     *
     * @param token                 token of the user requesting the operation
     * @param compartmentIdentifier Shongo identifier of the compartment to delete
     */
    @API
    public void deleteCompartment(SecurityToken token, String compartmentIdentifier)
            throws FaultException;

    /**
     * Lists all the compartments.
     *
     * @param token token of the user requesting the operation
     * @return collection of compartments
     */
    @API
    public Collection<CompartmentSummary> listCompartments(SecurityToken token);

    /**
     * Gets the complete compartment object.
     *
     * @param token                 token of the user requesting the operation
     * @param compartmentIdentifier identifier of the compartment to get
     */
    @API
    public Compartment getCompartment(SecurityToken token, String compartmentIdentifier)
            throws FaultException;
}
