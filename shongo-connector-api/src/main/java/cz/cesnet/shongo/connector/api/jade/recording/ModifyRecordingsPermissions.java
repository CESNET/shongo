package cz.cesnet.shongo.connector.api.jade.recording;

import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.api.jade.RecordingObjectType;
import cz.cesnet.shongo.api.jade.RecordingPermissionType;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.RecordingService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 * @see {@link cz.cesnet.shongo.connector.api.RecordingService}
 */
public class ModifyRecordingsPermissions extends ConnectorCommand {
    private String recordingFolderId;

    private RecordingObjectType targetType;

    private RecordingPermissionType permissionType;

    public ModifyRecordingsPermissions()
    {
    }

    public ModifyRecordingsPermissions(String recordingFolderId, RecordingObjectType targetType, RecordingPermissionType permissionType)
    {
        this.recordingFolderId = recordingFolderId;
        this.targetType = targetType;
        this.permissionType = permissionType;
    }

    public String getRecordingFolderId()
    {
        return recordingFolderId;
    }

    public void setRecordingFolderId(String recordingFolderId)
    {
        this.recordingFolderId = recordingFolderId;
    }

    public RecordingPermissionType getPermissionType() {
        return permissionType;
    }

    public void setPermissionType(RecordingPermissionType permissionType) {
        this.permissionType = permissionType;
    }

    public RecordingObjectType getTargetType() {
        return targetType;
    }

    public void setTargetType(RecordingObjectType targetType) {
        this.targetType = targetType;
    }


    @Override
    public Object execute(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        RecordingService recordingService = getRecording(connector);
        switch (targetType) {
            case FOLDER:
                if (permissionType.equals(RecordingPermissionType.PUBLIC)) {
                    recordingService.makeRecordingFolderPublic(recordingFolderId);
                } else if (permissionType.equals(RecordingPermissionType.PRIVATE)) {
                    recordingService.makeRecordingFolderPrivate(recordingFolderId);
                }
                break;
            case RECORDING:
                if (permissionType.equals(RecordingPermissionType.PUBLIC)) {
                    recordingService.makeRecordingPublic(recordingFolderId);
                } else if (permissionType.equals(RecordingPermissionType.PRIVATE)) {
                    recordingService.makeRecordingPrivate(recordingFolderId);
                }
                break;
        }
        return null;
    }

    @Override
    public String toString()
    {
        return String.format(ModifyRecordingsPermissions.class.getSimpleName() + " (recordingFolderId: %s)", recordingFolderId);
    }
}
