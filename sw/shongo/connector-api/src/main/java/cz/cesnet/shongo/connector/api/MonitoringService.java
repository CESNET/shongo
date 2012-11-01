package cz.cesnet.shongo.connector.api;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public interface MonitoringService
{
    /**
     * Gets info about current load of the device.
     *
     * @return current load info
     */
    DeviceLoadInfo getDeviceLoadInfo() throws CommandException, CommandUnsupportedException;

    /**
     * Gets the multipoint usage stats.
     *
     * @return usage stats
     */
    UsageStats getUsageStats() throws CommandException, CommandUnsupportedException;

    /**
     * Gets a snapshot of the video stream received by a user in a room.
     *
     * @param roomId     room identifier where the user resides
     * @param roomUserId identifier of the user within the room
     * @return image data; see the contentType of the returned object to get the image format
     */
    MediaData getReceivedVideoSnapshot(String roomId, String roomUserId) throws CommandException,
                                                                                CommandUnsupportedException;

    /**
     * Gets a snapshot of the video stream that a user is sending to the room.
     *
     * @param roomId     room identifier where the user resides
     * @param roomUserId identifier of the user within the room
     * @return image data; see the contentType of the returned object to get the image format
     */
    MediaData getSentVideoSnapshot(String roomId, String roomUserId) throws CommandException, CommandUnsupportedException;
}
