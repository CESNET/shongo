package cz.cesnet.shongo.connector.api;

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
    RoomInfo getRoomInfo(String roomId);

    /**
     * Create a new virtual room on a multipoint device that is managed by this connector.
     *
     * @param room room settings
     * @return identifier of the created room, unique within the device, to be used for further identification of the
     *         room as the roomId parameter
     */
    String createRoom(Room room);

    /**
     * Modifies a virtual room.
     *
     * @param roomId     room identifier
     * @param attributes map of room attributes to change
     */
    void modifyRoom(String roomId, Map attributes);

    /**
     * Deletes a virtual room.
     *
     * @param roomId room identifier
     */
    void deleteRoom(String roomId);

    /**
     * Gets current settings of a room exported to XML.
     *
     * @param roomId room identifier
     * @return room settings in XML
     */
    String exportRoomSettings(String roomId);

    /**
     * Sets up a room according to given settings previously exported by the <code>exportRoomSettings</code> method.
     *
     * @param roomId   room identifier
     * @param settings room settings in XML, previously returned by the exportRoomSettings method
     */
    void importRoomSettings(String roomId, String settings);
}
