package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.api.rpc.Service;
import cz.cesnet.shongo.controller.api.Executable;
import cz.cesnet.shongo.controller.api.ExecutableSummary;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.fault.FaultException;

import java.util.Collection;
import java.util.Map;

/**
 * Interface to the service handling operations on {@link cz.cesnet.shongo.controller.api.Executable}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface ExecutorService extends Service
{
    /**
     * Deletes a given compartment.
     *
     * @param token        token of the user requesting the operation
     * @param executableId shongo-id of the {@link cz.cesnet.shongo.controller.api.Executable} to delete
     */
    @API
    public void deleteExecutable(SecurityToken token, String executableId)
            throws FaultException;

    /**
     * Lists all the {@link cz.cesnet.shongo.controller.api.Executable}s.
     *
     * @param token  token of the user requesting the operation
     * @param filter attributes for filtering {@link cz.cesnet.shongo.controller.api.Executable}s (map of name => value pairs):
     *               -none for now
     * @return collection of {@link cz.cesnet.shongo.controller.api.ExecutableSummary}s
     */
    @API
    public Collection<ExecutableSummary> listExecutables(SecurityToken token, Map<String, Object> filter)
        throws FaultException;

    /**
     * Gets the complete compartment object.
     *
     * @param token        token of the user requesting the operation
     * @param executableId shongo-id of the {@link cz.cesnet.shongo.controller.api.Executable} to get
     */
    @API
    public Executable getExecutable(SecurityToken token, String executableId)
            throws FaultException;
}
