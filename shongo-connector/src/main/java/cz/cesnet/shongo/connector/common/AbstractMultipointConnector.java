package cz.cesnet.shongo.connector.common;

import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.connector.api.MultipointService;

/**
 * {@link AbstractConnector} for multipoint devices with the ability to {@link #recreateRoom}.
 *
 * @author @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractMultipointConnector extends AbstractDeviceConnector implements MultipointService
{
    @Override
    public abstract String createRoom(Room room) throws CommandException;

    @Override
    public abstract void deleteRoom(String roomId) throws CommandException;

    @Override
    public abstract Room getRoom(String roomId) throws CommandException;

    /**
     * Implementation of {@link MultipointService#modifyRoom} which allows for room recreation
     * when {@link #isRecreateNeeded} returns {@code true}.
     *
     * @param room room to be modified
     * @return new identifier of the room (it can be the same)
     * @throws CommandException when the modification fails
     */
    @Override
    public final String modifyRoom(Room room) throws CommandException
    {
        String roomId = room.getId();
        Room oldRoom = getRoom(roomId);
        if (oldRoom == null) {
            throw new CommandException("Room " + roomId + " doesn't exist.");
        }
        // If recreation is needed, recreate the room
        if (isRecreateNeeded(oldRoom, room)) {
            return recreateRoom(oldRoom, room);
        }
        // Otherwise just modify the room
        else {
            onModifyRoom(room);
            return roomId;
        }
    }

    /**
     * @param oldRoom
     * @param newRoom
     * @return true whether {@link #recreateRoom} is needed for modification from {@code oldRoom} to {@code newRoom},
     *         false otherwise
     */
    protected boolean isRecreateNeeded(Room oldRoom, Room newRoom) throws CommandException
    {
        return false;
    }

    /**
     * Perform the modification of {@code room} when it doesn't need the {@link #recreateRoom}.
     *
     * @param room to be modified
     * @throws CommandException when the modification fails
     */
    protected abstract void onModifyRoom(Room room) throws CommandException;

    /**
     * Create new room and while deleting the old one.
     * <p/>
     * Is necessary when some unique attribute of the room is changed.
     *
     * @param oldRoom
     * @param newRoom
     * @return identifier of the new room
     */
    protected String recreateRoom(Room oldRoom, Room newRoom) throws CommandException
    {
        // Delete old room
        String oldRoomId = oldRoom.getId();
        deleteRoom(oldRoomId);

        // Create new room
        return createRoom(newRoom);
    }
}
