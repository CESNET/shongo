package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.api.rpc.Service;
import cz.cesnet.shongo.controller.api.Executable;
import cz.cesnet.shongo.controller.api.ExecutableSummary;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.request.ExecutableListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;

import java.util.Collection;
import java.util.Map;

/**
 * Interface to the service handling operations on {@link cz.cesnet.shongo.controller.api.Executable}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface ExecutableService extends Service
{
    /**
     * Deletes a given compartment.
     *
     * @param token        token of the user requesting the operation
     * @param executableId shongo-id of the {@link cz.cesnet.shongo.controller.api.Executable} to delete
     */
    @API
    public void deleteExecutable(SecurityToken token, String executableId);

    /**
     * Lists all the {@link cz.cesnet.shongo.controller.api.Executable}s.
     *
     * @param request {@link ExecutableListRequest}
     * @return {@link ListResponse} of {@link ExecutableSummary}s
     */
    @API
    public ListResponse<ExecutableSummary> listExecutables(ExecutableListRequest request);

    /**
     * Gets the complete compartment object.
     *
     * @param token        token of the user requesting the operation
     * @param executableId shongo-id of the {@link cz.cesnet.shongo.controller.api.Executable} to get
     */
    @API
    public Executable getExecutable(SecurityToken token, String executableId);

    /**
     * Try to start/stop/update given {@link Executable} (e.g., if it is in failed state).
     *
     * @param token        token of the user requesting the operation
     * @param executableId shongo-id of the {@link cz.cesnet.shongo.controller.api.Executable} to start
     */
    @API
    public void updateExecutable(SecurityToken token, String executableId);
}
