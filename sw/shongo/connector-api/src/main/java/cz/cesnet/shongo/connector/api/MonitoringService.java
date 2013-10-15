package cz.cesnet.shongo.connector.api;

import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public interface MonitoringService
{
    /**
     * Gets the multipoint usage stats.
     *
     * @return usage stats
     */
    UsageStats getUsageStats() throws CommandException, CommandUnsupportedException;

    /**
     * Gets a snapshot of the video stream received by a user in a room.
     *
     * @param roomId            room identifier where the user resides
     * @param roomParticipantId identifier of the user within the room
     * @return image data; see the contentType of the returned object to get the image format
     */
    MediaData getReceivedVideoSnapshot(String roomId, String roomParticipantId) throws CommandException,
                                                                                       CommandUnsupportedException;

    /**
     * Gets a snapshot of the video stream that a user is sending to the room.
     *
     * @param roomId            room identifier where the user resides
     * @param roomParticipantId identifier of the user within the room
     * @return image data; see the contentType of the returned object to get the image format
     */
    MediaData getSentVideoSnapshot(String roomId, String roomParticipantId)
            throws CommandException, CommandUnsupportedException;
}
