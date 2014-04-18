package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.api.rpc.Service;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.ExecutableListRequest;
import cz.cesnet.shongo.controller.api.request.ExecutableRecordingListRequest;
import cz.cesnet.shongo.controller.api.request.ExecutableServiceListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;

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
     * @param executableId  shongo-id of the {@link Executable} to get
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
     * @param executableId            shongo-id of the {@link Executable} to get
     * @param executableConfiguration new configuration for an executable with the given {@code executableId}
     */
    @API
    public void modifyExecutableConfiguration(SecurityToken securityToken, String executableId,
            ExecutableConfiguration executableConfiguration);

    /**
     * Deletes a executable with given {@code executableId}.
     *
     * @param securityToken token of the user requesting the operation
     * @param executableId  shongo-id of the {@link Executable} to delete
     */
    @API
    public void deleteExecutable(SecurityToken securityToken, String executableId);

    /**
     * Try to again start/update/stop {@link Executable} with given {@code executableId} (e.g., if it is in failed state).
     *
     * @param securityToken token of the user requesting the operation
     * @param executableId  shongo-id of the {@link Executable} to start/update/stop
     * @param skipExecution specifies whether the actual execution (start/update/stop) should be skipped and only the state
     *                      should be set to proper value
     */
    @API
    public void updateExecutable(SecurityToken securityToken, String executableId, Boolean skipExecution);

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
     * @return {@link Boolean#TRUE} when the activation succeeds,
     *         {@link Boolean#FALSE} when the service was already active,
     *         otherwise {@link ExecutionReport} describing the error
     */
    @API
    public Object activateExecutableService(SecurityToken securityToken, String executableId, String executableServiceId);

    /**
     * Deactivate service with given {@code executableServiceId} for executable with given {@code executableId}.
     *
     * @param executableId
     * @param executableServiceId
     * @return {@link Boolean#TRUE} when the deactivation succeeds,
     *         {@link Boolean#FALSE} when the service was already inactive,
     *         otherwise {@link ExecutionReport} describing the error
     */
    @API
    public Object deactivateExecutableService(SecurityToken securityToken, String executableId, String executableServiceId);

    /**
     * Lists {@link cz.cesnet.shongo.controller.api.ResourceRecording}s for {@link Executable} with given {@link ExecutableRecordingListRequest#executableId}.
     *
     * @param request {@link ExecutableRecordingListRequest}
     * @return {@link ListResponse} of {@link cz.cesnet.shongo.controller.api.ResourceRecording}s
     */
    @API
    public ListResponse<ResourceRecording> listExecutableRecordings(ExecutableRecordingListRequest request);
}
