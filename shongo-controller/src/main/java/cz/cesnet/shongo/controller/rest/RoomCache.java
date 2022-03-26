
package cz.cesnet.shongo.controller.rest;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.ExpirationMap;
import cz.cesnet.shongo.api.MediaData;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.RoomParticipant;
import cz.cesnet.shongo.controller.api.Executable;
import cz.cesnet.shongo.controller.api.RoomExecutable;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.rpc.ExecutableService;
import cz.cesnet.shongo.controller.api.rpc.ResourceControlService;
import cz.cesnet.shongo.controller.rest.models.UnsupportedApiException;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * Cache of information for management of rooms.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RoomCache
{
    private static Logger logger = LoggerFactory.getLogger(RoomCache.class);

    private final ResourceControlService resourceControlService;

    private final ExecutableService executableService;

    private final Cache cache;

    /**
     * {@link RoomExecutable} by roomExecutableId.
     */
    private final ExpirationMap<String, RoomExecutable> roomExecutableCache =
            new ExpirationMap<String, RoomExecutable>();

    /**
     * {@link Room} by roomExecutableId".
     */
    private final ExpirationMap<String, Room> roomCache =
            new ExpirationMap<String, Room>();

    /**
     * Collection of {@link RoomParticipant}s by roomExecutableId.
     */
    private final ExpirationMap<String, List<RoomParticipant>> roomParticipantsCache =
            new ExpirationMap<String, List<RoomParticipant>>();

    /**
     * {@link RoomParticipant} by "roomExecutableId:participantId".
     */
    private final ExpirationMap<String, RoomParticipant> roomParticipantCache =
            new ExpirationMap<String, RoomParticipant>();

    /**
     * Participant snapshots in {@link MediaData} by "roomExecutableId:participantId".
     */
    private final ExpirationMap<String, MediaData> roomParticipantSnapshotCache =
            new ExpirationMap<String, MediaData>();

    /**
     * Constructor.
     */
    public RoomCache(
            @Autowired ResourceControlService resourceControlService,
            @Autowired ExecutableService executableService,
            @Autowired Cache cache)
    {
        this.resourceControlService = resourceControlService;
        this.executableService = executableService;
        this.cache = cache;

        // Set expiration durations
        roomCache.setExpiration(Duration.standardSeconds(30));
        roomParticipantsCache.setExpiration(Duration.standardSeconds(15));
        roomExecutableCache.setExpiration(Duration.standardSeconds(15));
        roomParticipantSnapshotCache.setExpiration(Duration.standardSeconds(15));
    }

    /**
     * @param securityToken
     * @param roomExecutableId
     * @return {@link Room} for given {@code roomExecutableId}
     */
    public Room getRoom(SecurityToken securityToken, String roomExecutableId)
    {
        synchronized (roomCache) {
            Room room = roomCache.get(roomExecutableId);
            if (room == null) {
                RoomExecutable roomExecutable = getRoomExecutable(securityToken, roomExecutableId);
                String resourceId = roomExecutable.getResourceId();
                String resourceRoomId = roomExecutable.getRoomId();
                room = resourceControlService.getRoom(securityToken, resourceId, resourceRoomId);
                roomCache.put(roomExecutableId, room);
            }
            return room;
        }
    }

    /**
     * Modify given {@code room}.
     *
     * @param securityToken
     * @param room
     */
    public void modifyRoom(SecurityToken securityToken, String roomExecutableId, Room room)
    {
        synchronized (roomCache) {
            RoomExecutable roomExecutable = getRoomExecutable(securityToken, roomExecutableId);
            String resourceId = roomExecutable.getResourceId();
            if (!room.getId().equals(roomExecutable.getRoomId())) {
                throw new IllegalArgumentException("Room doesn't correspond to given executable.");
            }
            resourceControlService.modifyRoom(securityToken, resourceId, room);
            roomCache.put(roomExecutableId, room);
        }
    }

    /**
     * @param securityToken
     * @param roomExecutableId
     * @return collection of {@link RoomParticipant}s for given {@code roomExecutableId}
     */
    public List<RoomParticipant> getRoomParticipants(SecurityToken securityToken, String roomExecutableId)
    {
        synchronized (roomParticipantsCache) {
            List<RoomParticipant> roomParticipants = roomParticipantsCache.get(roomExecutableId);
            if (roomParticipants == null) {
                RoomExecutable roomExecutable = getRoomExecutable(securityToken, roomExecutableId);
                String resourceId = roomExecutable.getResourceId();
                String resourceRoomId = roomExecutable.getRoomId();
                roomParticipants = new LinkedList<>();
                synchronized (roomParticipantCache) {
                    for (RoomParticipant roomParticipant : resourceControlService.listRoomParticipants(
                            securityToken, resourceId, resourceRoomId)) {
                        roomParticipants.add(roomParticipant);
                        roomParticipantCache.put(roomExecutableId + ":" + roomParticipant.getId(), roomParticipant);
                    }
                }
                roomParticipantsCache.put(roomExecutableId, roomParticipants);
            }
            return roomParticipants;
        }
    }

    /**
     * @param securityToken
     * @param roomExecutableId
     * @param roomParticipantId
     * @return {@link RoomParticipant} for given {@code roomExecutableId} and {@code roomParticipantId}
     */
    public RoomParticipant getRoomParticipant(SecurityToken securityToken, String roomExecutableId, String roomParticipantId)
    {
        String cacheId = roomExecutableId + ":" + roomParticipantId;
        synchronized (roomParticipantCache) {
            RoomParticipant roomParticipant = roomParticipantCache.get(cacheId);
            if (roomParticipant == null) {
                RoomExecutable roomExecutable = getRoomExecutable(securityToken, roomExecutableId);
                String resourceId = roomExecutable.getResourceId();
                String resourceRoomId = roomExecutable.getRoomId();
                roomParticipant = resourceControlService.getRoomParticipant(
                        securityToken, resourceId, resourceRoomId, roomParticipantId);
                if (roomParticipant == null) {
                    throw new CommonReportSet.ObjectNotExistsException("RoomParticipant", roomParticipantId);
                }
                roomParticipantCache.put(cacheId, roomParticipant);
            }
            return roomParticipant;
        }
    }

    /**
     * @param securityToken
     * @param roomExecutableId
     * @param roomParticipant to be modified in the given {@code roomExecutableId}
     */
    public void modifyRoomParticipant(SecurityToken securityToken, String roomExecutableId, RoomParticipant roomParticipant)
    {
        RoomExecutable roomExecutable = getRoomExecutable(securityToken, roomExecutableId);
        String resourceId = roomExecutable.getResourceId();
        String resourceRoomId = roomExecutable.getRoomId();
        roomParticipant.setRoomId(resourceRoomId);
        resourceControlService.modifyRoomParticipant(securityToken, resourceId, roomParticipant);
        synchronized (roomParticipantCache) {
            roomParticipantCache.remove(roomParticipant.getId());
        }
        synchronized (roomParticipantsCache) {
            roomParticipantsCache.remove(roomExecutableId);
        }
    }

    /**
     * @param securityToken
     * @param roomExecutableId
     * @param roomParticipants configuration to which all room participants should be modified in the given {@code roomExecutableId}
     */
    public void modifyRoomParticipants(SecurityToken securityToken, String roomExecutableId, RoomParticipant roomParticipants)
    {
        RoomExecutable roomExecutable = getRoomExecutable(securityToken, roomExecutableId);
        String resourceId = roomExecutable.getResourceId();
        String resourceRoomId = roomExecutable.getRoomId();
        roomParticipants.setRoomId(resourceRoomId);
        resourceControlService.modifyRoomParticipants(securityToken, resourceId, roomParticipants);
        synchronized (roomParticipantCache) {
            List<RoomParticipant> participants = roomParticipantsCache.get(roomExecutableId);
            if (participants != null) {
                for (RoomParticipant roomParticipant : participants) {
                    roomParticipantCache.remove(roomExecutableId + ":" + roomParticipant.getId());
                }
            }
        }
        synchronized (roomParticipantsCache) {
            roomParticipantsCache.remove(roomExecutableId);
        }
    }

    /**
     * @param securityToken
     * @param roomExecutableId
     * @param roomParticipantId
     * @return {@link MediaData} snapshot of room participant
     */
    public MediaData getRoomParticipantSnapshot(SecurityToken securityToken, String roomExecutableId, String roomParticipantId)
    {
        String cacheId = roomExecutableId + ":" + roomParticipantId;
        synchronized (roomParticipantSnapshotCache) {
            if (!roomParticipantSnapshotCache.contains(cacheId)) {
                RoomExecutable roomExecutable = getRoomExecutable(securityToken, roomExecutableId);
                String resourceId = roomExecutable.getResourceId();
                String resourceRoomId = roomExecutable.getRoomId();
                Set<String> roomParticipantIds = new HashSet<String>();
                roomParticipantIds.add(roomParticipantId);
                Map<String, MediaData> participantSnapshots = resourceControlService.getRoomParticipantSnapshots(
                        securityToken, resourceId, resourceRoomId, roomParticipantIds);
                MediaData roomParticipantSnapshot = participantSnapshots.get(roomParticipantId);
                roomParticipantSnapshotCache.put(cacheId, roomParticipantSnapshot);
            }
            return roomParticipantSnapshotCache.get(cacheId);
        }
    }

    /**
     * @param securityToken
     * @param roomExecutableId
     * @param roomParticipantId to be disconnected from given {@code roomExecutableId}
     */
    public void disconnectRoomParticipant(SecurityToken securityToken, String roomExecutableId, String roomParticipantId)
    {
        RoomExecutable roomExecutable = getRoomExecutable(securityToken, roomExecutableId);
        String resourceId = roomExecutable.getResourceId();
        String resourceRoomId = roomExecutable.getRoomId();
        resourceControlService.disconnectRoomParticipant(securityToken, resourceId, resourceRoomId, roomParticipantId);
        synchronized (roomParticipantCache) {
            roomParticipantCache.remove(roomParticipantId);
        }
        synchronized (roomParticipantsCache) {
            roomParticipantsCache.remove(roomExecutableId);
        }
    }

    /**
     * @param securityToken
     * @param roomExecutableId
     * @return {@link RoomExecutable} for given {@code roomExecutableId}
     */
    public RoomExecutable getRoomExecutable(SecurityToken securityToken, String roomExecutableId)
    {
        synchronized (roomExecutableCache) {
            RoomExecutable roomExecutable = roomExecutableCache.get(roomExecutableId);
            if (roomExecutable == null) {
                Executable executable = cache.getExecutable(securityToken, roomExecutableId);
                if (executable instanceof RoomExecutable) {
                    roomExecutable = (RoomExecutable) executable;
                }
                else {
                    throw new UnsupportedApiException(executable);
                }
                roomExecutableCache.put(roomExecutableId, roomExecutable);
            }
            return roomExecutable;
        }
    }
}
