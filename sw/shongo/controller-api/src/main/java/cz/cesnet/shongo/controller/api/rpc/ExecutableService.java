package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.api.Recording;
import cz.cesnet.shongo.api.rpc.Service;
import cz.cesnet.shongo.controller.api.Executable;
import cz.cesnet.shongo.controller.api.ExecutableConfiguration;
import cz.cesnet.shongo.controller.api.ExecutableSummary;
import cz.cesnet.shongo.controller.api.ExecutionReport;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.request.ExecutableListRequest;
import cz.cesnet.shongo.controller.api.request.ExecutableRecordingListRequest;
import cz.cesnet.shongo.controller.api.request.ExecutableServiceListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;

import java.util.Collection;

/**
 * Interface to the service handling operations on {@link Executable}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface ExecutableService extends Service
{
    /**
     * Lists all the {@link Executable}s.
     *
     * @param request {@link ExecutableListRequest}
     * @return {@link ListResponse} of {@link ExecutableSummary}s
     */
    @API
    public ListResponse<ExecutableSummary> listExecutables(ExecutableListRequest request);

    /**
     * Gets the {@link Executable} for given {@code executableId}.
     *
     * @param securityToken token of the user requesting the operation
     * @param executableId  shongo-id of the {@link cz.cesnet.shongo.controller.api.Executable} to get
     */
    @API
    public Executable getExecutable(SecurityToken securityToken, String executableId);

    /**
     * Lists {@link ExecutableService}s for {@link Executable} with given {@link ExecutableServiceListRequest#executableId}.
     *
     * @param request {@link ExecutableServiceListRequest}
     * @return {@link ListResponse} of {@link ExecutableService}s
     */
    @API
    public ListResponse<cz.cesnet.shongo.controller.api.ExecutableService> listExecutableServices(
            ExecutableServiceListRequest request);

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

    /**
     * Attach {@link cz.cesnet.shongo.controller.api.ExecutableState#NOT_STARTED} room executable to an existing room in
     * target device.
     *
     * @param securityToken    token of the user requesting the operation
     * @param roomExecutableId identifier of room executable
     * @param deviceRoomId     identifier of room in target device
     */
    @API
    public void attachRoomExecutable(SecurityToken securityToken, String roomExecutableId, String deviceRoomId);

    /**
     * Activate service with given {@code executableServiceId} for executable with given {@code executableId}.
     *
     * @param executableId
     * @param executableServiceId
     * @return {@link Boolean#TRUE} when the activation succeeds
     *         otherwise {@link ExecutionReport} describing the error
     */
    @API
    public Object activateExecutableService(SecurityToken securityToken, String executableId, String executableServiceId);

    /**
     * Deactivate service with given {@code executableServiceId} for executable with given {@code executableId}.
     *
     * @param executableId
     * @param executableServiceId
     * @return {@link Boolean#TRUE} when the deactivation succeeds
     *         otherwise {@link ExecutionReport} describing the error
     */
    @API
    public Object deactivateExecutableService(SecurityToken securityToken, String executableId, String executableServiceId);

    /**
     * Lists {@link Recording}s for {@link Executable} with given {@link ExecutableRecordingListRequest#executableId}.
     *
     * @param request {@link ExecutableRecordingListRequest}
     * @return {@link ListResponse} of {@link Recording}s
     */
    @API
    public ListResponse<Recording> listExecutableRecordings(ExecutableRecordingListRequest request);
}
