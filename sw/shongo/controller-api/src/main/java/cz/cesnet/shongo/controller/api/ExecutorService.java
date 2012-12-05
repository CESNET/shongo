package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.xmlrpc.Service;
import cz.cesnet.shongo.fault.FaultException;

import java.util.Collection;
import java.util.Map;

/**
 * Interface to the service handling operations on {@link Executable}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface ExecutorService extends Service
{
    /**
     * Deletes a given compartment.
     *
     * @param token                token of the user requesting the operation
     * @param executableIdentifier Shongo identifier of the {@link Executable} to delete
     */
    @API
    public void deleteExecutable(SecurityToken token, String executableIdentifier)
            throws FaultException;

    /**
     * Lists all the {@link Executable}s.
     *
     * @param token  token of the user requesting the operation
     * @param filter attributes for filtering {@link Executable}s (map of name => value pairs)::
     *               -{@code userId}     restricts {@link Executable} owner by his identifier
     * @return collection of {@link ExecutableSummary}s
     */
    @API
    public Collection<ExecutableSummary> listExecutables(SecurityToken token, Map<String, Object> filter);

    /**
     * Gets the complete compartment object.
     *
     * @param token                token of the user requesting the operation
     * @param executableIdentifier identifier of the {@link Executable} to get
     */
    @API
    public Executable getExecutable(SecurityToken token, String executableIdentifier)
            throws FaultException;
}
