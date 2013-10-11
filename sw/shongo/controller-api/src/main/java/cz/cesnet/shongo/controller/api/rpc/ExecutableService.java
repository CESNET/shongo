package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.api.rpc.Service;
import cz.cesnet.shongo.controller.api.Executable;
import cz.cesnet.shongo.controller.api.ExecutableConfiguration;
import cz.cesnet.shongo.controller.api.ExecutableSummary;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.request.ExecutableListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;

/**
 * Interface to the service handling operations on {@link cz.cesnet.shongo.controller.api.Executable}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface ExecutableService extends Service
{
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
     * @param securityToken token of the user requesting the operation
     * @param executableId  shongo-id of the {@link cz.cesnet.shongo.controller.api.Executable} to get
     */
    @API
    public Executable getExecutable(SecurityToken securityToken, String executableId);

    /**
     * Updates executable configuration.
     *
     * @param securityToken           token of the user requesting the operation
     * @param executableId            shongo-id of the {@link cz.cesnet.shongo.controller.api.Executable} to get
     * @param executableConfiguration new configuration for an executable with the given {@code executableId}
     */
    @API
    public void modifyExecutableConfiguration(SecurityToken securityToken, String executableId,
            ExecutableConfiguration executableConfiguration);

    /**
     * Deletes a executable with given {@code executableId}.
     *
     * @param securityToken token of the user requesting the operation
     * @param executableId  shongo-id of the {@link cz.cesnet.shongo.controller.api.Executable} to delete
     */
    @API
    public void deleteExecutable(SecurityToken securityToken, String executableId);

    /**
     * Try to start/stop/update again given {@link Executable} (e.g., if it is in failed state).
     *
     * @param securityToken token of the user requesting the operation
     * @param executableId  shongo-id of the {@link cz.cesnet.shongo.controller.api.Executable} to start
     */
    @API
    public void updateExecutable(SecurityToken securityToken, String executableId);
}
