package cz.cesnet.shongo.controller.booking.recording;

import cz.cesnet.shongo.connector.api.jade.recording.StartRecording;
import cz.cesnet.shongo.connector.api.jade.recording.StopRecording;
import cz.cesnet.shongo.controller.ControllerAgent;
import cz.cesnet.shongo.controller.booking.EntityIdentifier;
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
public class RecordingService extends ExecutableService
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

    @PreUpdate
    protected void onUpdate()
    {
        boolean isActive = getState().equals(State.ACTIVE);
        if (isActive && recordingId == null) {
            throw new IllegalStateException("Active recording service should have recording identifier.");
        }
        else if (!isActive && recordingId != null) {
            throw new IllegalStateException("Inactive recording service shouldn't have recording identifier.");
        }
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

            executor.getLogger().warn("!!!FILL StartRecording ARGUMENTS!!!");
            SendLocalCommand sendLocalCommand = controllerAgent.sendCommand(agentName, new StartRecording());
            if (sendLocalCommand.getState() == SendLocalCommand.State.SUCCESSFUL) {
                recordingId = (String) sendLocalCommand.getResult();
                if (recordingId == null) {
                    throw new IllegalStateException("StartRecording should return identifier of the new recording.");
                }
                return State.ACTIVE;
            }
            else {
                executableManager.createExecutionReport(this, new ExecutorReportSet.CommandFailedReport(
                        sendLocalCommand.getName(), sendLocalCommand.getJadeReport()));
                return State.ACTIVATION_FAILED;
            }
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
            SendLocalCommand sendLocalCommand = controllerAgent.sendCommand(agentName, new StopRecording(recordingId));
            if (sendLocalCommand.getState() == SendLocalCommand.State.SUCCESSFUL) {
                recordingId = null;
                return State.NOT_ACTIVE;
            }
            else {
                executableManager.createExecutionReport(this, new ExecutorReportSet.CommandFailedReport(
                        sendLocalCommand.getName(), sendLocalCommand.getJadeReport()));
                return State.DEACTIVATION_FAILED;
            }
        }
        else {
            throw new IllegalStateException("Device resource is not managed.");
        }
    }
}
