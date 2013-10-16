package cz.cesnet.shongo.connector.api;

import cz.cesnet.shongo.api.MediaData;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public interface RoomContentService
{
    /**
     * Gets all room content (e.g., documents, notes, polls, ...) as a single archive.
     *
     * @param roomId room identifier
     * @return an archive containing all the room content; see the <code>compression</code> attribute of the returned
     *         object
     */
    MediaData getRoomContent(String roomId) throws CommandException, CommandUnsupportedException;

    /**
     * Adds a data file to room content under a given name.
     *
     * @param roomId room identifier
     * @param name   name of file to add
     * @param data   data to add
     */
    void addRoomContent(String roomId, String name, MediaData data)
            throws CommandException, CommandUnsupportedException;

    /**
     * Removes a file of a given name from the room content.
     *
     * @param roomId room identifier
     * @param name   name of file to remove
     */
    void removeRoomContentFile(String roomId, String name) throws CommandException, CommandUnsupportedException;

    /**
     * Clears all room content.
     *
     * @param roomId room identifier
     */
    void clearRoomContent(String roomId) throws CommandException, CommandUnsupportedException;
}
