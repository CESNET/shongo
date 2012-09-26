package cz.cesnet.shongo.connector.api;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;

import java.util.Map;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public interface RoomService
{
    /**
     * Gets info about an existing room.
     *
     * @param roomId id of the room to get info about
     * @return information about a room with roomId
     */
    RoomInfo getRoomInfo(String roomId) throws CommandException, CommandUnsupportedException;

    /**
     * Create a new virtual room on a multipoint device that is managed by this connector.
     *
     * @param room room settings
     * @return identifier of the created room, unique within the device, to be used for further identification of the
     *         room as the roomId parameter
     */
    String createRoom(Room room) throws CommandException, CommandUnsupportedException;

    /**
     * Modifies a virtual room.
     *
     * @param roomId     room identifier
     * @param attributes map of room attributes to change
     */
    void modifyRoom(String roomId, Map attributes) throws CommandException, CommandUnsupportedException;

    /**
     * Deletes a virtual room.
     *
     * @param roomId room identifier
     */
    void deleteRoom(String roomId) throws CommandException, CommandUnsupportedException;

    /**
     * Gets current settings of a room exported to XML.
     *
     * @param roomId room identifier
     * @return room settings in XML
     */
    String exportRoomSettings(String roomId) throws CommandException, CommandUnsupportedException;

    /**
     * Sets up a room according to given settings previously exported by the <code>exportRoomSettings</code> method.
     *
     * @param roomId   room identifier
     * @param settings room settings in XML, previously returned by the exportRoomSettings method
     */
    void importRoomSettings(String roomId, String settings) throws CommandException, CommandUnsupportedException;
}
