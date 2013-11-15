package cz.cesnet.shongo.controller.booking.recording;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.connector.api.jade.recording.CreateRecordingFolder;
import cz.cesnet.shongo.connector.api.jade.recording.StartRecording;
import cz.cesnet.shongo.connector.api.jade.recording.StopRecording;
import cz.cesnet.shongo.controller.ControllerAgent;
import cz.cesnet.shongo.controller.booking.EntityIdentifier;
import cz.cesnet.shongo.controller.booking.alias.Alias;
import cz.cesnet.shongo.controller.booking.executable.EndpointExecutableService;
import cz.cesnet.shongo.controller.booking.executable.ExecutableManager;
import cz.cesnet.shongo.controller.booking.executable.ExecutableService;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.booking.resource.ManagedMode;
import cz.cesnet.shongo.controller.executor.Executor;
import cz.cesnet.shongo.controller.executor.ExecutorReportSet;
import cz.cesnet.shongo.jade.SendLocalCommand;

import javax.persistence.*;

/**
 * {@link cz.cesnet.shongo.controller.booking.executable.ExecutableService} for recording.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class RecordingService extends ExecutableService implements EndpointExecutableService
{
    /**
     * {@link RecordingCapability} of {@link DeviceResource} which is used for recording.
     */
    private RecordingCapability recordingCapability;

    /**
     * Current identifier of {@link cz.cesnet.shongo.api.Recording}.
     */
    private String recordingId;

    /**
     * Constructor.
     */
    public RecordingService()
    {
    }

    /**
     * @return {@link #recordingCapability}
     */
    @ManyToOne(optional = false)
    @Access(AccessType.FIELD)
    public RecordingCapability getRecordingCapability()
    {
        return recordingCapability;
    }

    /**
     * @return {@link #executable} as {@link RecordableEndpoint}
     */
    @Transient
    public RecordableEndpoint getRecordingEndpoint()
    {
        if (executable instanceof RecordableEndpoint) {
            return (RecordableEndpoint) executable;
        }
        else {
            throw new TodoImplementException(
                    executable.getClass() + " doesn't implement " + RecordableEndpoint.class.getSimpleName() + ".");
        }
    }

    /**
     * @param recordingCapability sets the {@link #recordingCapability}
     */
    public void setRecordingCapability(RecordingCapability recordingCapability)
    {
        this.recordingCapability = recordingCapability;
    }

    /**
     * @return {@link #recordingId}
     */
    public String getRecordingId()
    {
        return recordingId;
    }

    /**
     * @param recordingId sets the {@link #recordingId}
     */
    public void setRecordingId(String recordingId)
    {
        this.recordingId = recordingId;
    }

    @Transient
    @Override
    public boolean isEndpoint()
    {
        // If recording capability can record only limited number of recordings at the same time,
        // it means that the service is probably an endpoint
        // TODO: add some more meaningful attribute to RecordingCapability (e.g., recordingServiceIsEndpoint)
        return recordingCapability.getLicenseCount() != null;
    }

    @Override
    protected cz.cesnet.shongo.controller.api.ExecutableService createApi()
    {
        return new cz.cesnet.shongo.controller.api.RecordingService();
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.ExecutableService executableServiceApi)
    {
        super.toApi(executableServiceApi);

        cz.cesnet.shongo.controller.api.RecordingService recordingServiceApi =
                (cz.cesnet.shongo.controller.api.RecordingService) executableServiceApi;

        recordingServiceApi.setResourceId(EntityIdentifier.formatId(recordingCapability.getResource()));
        recordingServiceApi.setRecordingId(recordingId);
    }

    @Override
    protected State onActivate(Executor executor, ExecutableManager executableManager)
    {
        DeviceResource deviceResource = recordingCapability.getDeviceResource();
        if (deviceResource.isManaged()) {
            ManagedMode managedMode = (ManagedMode) deviceResource.getMode();
            String agentName = managedMode.getConnectorAgentName();
            ControllerAgent controllerAgent = executor.getControllerAgent();
            RecordableEndpoint recordableEndpoint = getRecordingEndpoint();

            // Super activation
            State state = super.onActivate(executor, executableManager);
            if (!State.ACTIVE.equals(state)) {
                return state;
            }

            // Prepare recording folder
            String recordingFolderId;
            synchronized (RecordableEndpoint.SYNCHRONIZATION.get(recordableEndpoint.getId())) {
                recordingFolderId = recordableEndpoint.getRecordingFolderId(recordingCapability);
                if (recordingFolderId == null) {
                    SendLocalCommand sendLocalCommand = controllerAgent.sendCommand(agentName, new CreateRecordingFolder(
                            recordableEndpoint.getRecordingFolderDescription()));
                    if (!SendLocalCommand.State.SUCCESSFUL.equals(sendLocalCommand.getState())) {
                        executableManager.createExecutionReport(this, new ExecutorReportSet.CommandFailedReport(
                                sendLocalCommand.getName(), sendLocalCommand.getJadeReport()));
                        return State.ACTIVATION_FAILED;
                    }
                    recordingFolderId = (String) sendLocalCommand.getResult();
                    recordableEndpoint.putRecordingFolderId(recordingCapability, recordingFolderId);
                }
            }

            // Start recording
            Alias alias = recordableEndpoint.getRecordingAlias();
            SendLocalCommand sendLocalCommand = controllerAgent.sendCommand(agentName,
                    new StartRecording(recordingFolderId, alias.toApi()));
            if (!SendLocalCommand.State.SUCCESSFUL.equals(sendLocalCommand.getState())) {
                executableManager.createExecutionReport(this, new ExecutorReportSet.CommandFailedReport(
                        sendLocalCommand.getName(), sendLocalCommand.getJadeReport()));
                return State.ACTIVATION_FAILED;
            }
            recordingId = (String) sendLocalCommand.getResult();
            if (recordingId == null) {
                throw new IllegalStateException("StartRecording should return identifier of the new recording.");
            }
            return State.ACTIVE;
        }
        else {
            throw new IllegalStateException("Device resource is not managed.");
        }
    }

    @Override
    protected State onDeactivate(Executor executor, ExecutableManager executableManager)
    {
        DeviceResource deviceResource = recordingCapability.getDeviceResource();
        if (deviceResource.isManaged()) {
            ManagedMode managedMode = (ManagedMode) deviceResource.getMode();
            String agentName = managedMode.getConnectorAgentName();
            ControllerAgent controllerAgent = executor.getControllerAgent();
            RecordableEndpoint recordableEndpoint = getRecordingEndpoint();

            // Stop recording
            if (recordingId != null) {
                SendLocalCommand sendLocalCommand = controllerAgent.sendCommand(agentName, new StopRecording(recordingId));
                if (!SendLocalCommand.State.SUCCESSFUL.equals(sendLocalCommand.getState())) {
                    executableManager.createExecutionReport(this, new ExecutorReportSet.CommandFailedReport(
                            sendLocalCommand.getName(), sendLocalCommand.getJadeReport()));
                    return State.DEACTIVATION_FAILED;
                }
                recordingId = null;
            }

            // Super deactivation
            return super.onDeactivate(executor, executableManager);
        }
        else {
            throw new IllegalStateException("Device resource is not managed.");
        }
    }
}
