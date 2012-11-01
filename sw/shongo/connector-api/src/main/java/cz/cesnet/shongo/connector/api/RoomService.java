package cz.cesnet.shongo.connector.api;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.RoomSummary;

import java.util.Collection;
import java.util.Map;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public interface RoomService
{
    /**
     * Lists all rooms at the device.
     *
     * @return array of rooms
     */
    Collection<RoomSummary> getRoomList() throws CommandException, CommandUnsupportedException;

    /**
     * Gets info about an existing room.
     *
     * @param roomId id of the room to get info about
     * @return information about a room with roomId
     */
    RoomSummary getRoomSummary(String roomId) throws CommandException, CommandUnsupportedException;

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
     * The attributes may name any of Room attributes (see constants in the Room class).
     *
     * @param roomId     room identifier
     * @param attributes map of room attributes to change; may be <code>null</code> for no changes in room attributes
     * @param options    map of room options to change; may be <code>null</code> for no changes in room options
     * @return new room identifier (shall be the same for most connectors, but may change due to changes in some
     *         attributes)
     */
    String modifyRoom(String roomId, Map<String, Object> attributes, Map<Room.Option, Object> options)
            throws CommandException, CommandUnsupportedException;

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
