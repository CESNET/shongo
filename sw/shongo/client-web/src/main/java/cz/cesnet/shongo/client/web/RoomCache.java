package cz.cesnet.shongo.client.web;

import cz.cesnet.shongo.ExpirationMap;
import cz.cesnet.shongo.api.MediaData;
import cz.cesnet.shongo.api.Recording;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.RoomParticipant;
import cz.cesnet.shongo.client.web.models.UnsupportedApiException;
import cz.cesnet.shongo.controller.api.Executable;
import cz.cesnet.shongo.controller.api.RoomExecutable;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.rpc.ExecutableService;
import cz.cesnet.shongo.controller.api.rpc.ResourceControlService;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.*;

/**
 * Cache of information for management of rooms.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RoomCache
{
    private static Logger logger = LoggerFactory.getLogger(RoomCache.class);

    @Resource
    private ResourceControlService resourceControlService;

    @Resource
    private ExecutableService executableService;

    /**
     * {@link RoomExecutable} by roomExecutableId".
     */
    private final ExpirationMap<String, RoomExecutable> roomExecutableCache =
            new ExpirationMap<String, RoomExecutable>();

    /**
     * {@link Room} by roomExecutableId".
     */
    private final ExpirationMap<String, Room> roomCache =
            new ExpirationMap<String, Room>();

    /**
     * Collection of {@link RoomParticipant}s by roomExecutableId".
     */
    private final ExpirationMap<String, List<RoomParticipant>> roomParticipantsCache =
            new ExpirationMap<String, List<RoomParticipant>>();

    /**
     * {@link RoomParticipant} by "roomExecutableId:participantId".
     */
    private final ExpirationMap<String, RoomParticipant> roomParticipantCache =
            new ExpirationMap<String, RoomParticipant>();

    /**
     * Collection of {@link Recording}s by roomExecutableId".
     */
    private final ExpirationMap<String, List<Recording>> roomRecordingsCache =
            new ExpirationMap<String, List<Recording>>();

    /**
     * Participant snapshots in {@link MediaData} by "roomExecutableId:participantId".
     */
    private final ExpirationMap<String, MediaData> roomParticipantSnapshotCache =
            new ExpirationMap<String, MediaData>();

    /**
     * Constructor.
     */
    public RoomCache()
    {
        // Set expiration durations
        roomCache.setExpiration(Duration.standardSeconds(20));
        roomParticipantsCache.setExpiration(Duration.standardSeconds(20));
        roomRecordingsCache.setExpiration(Duration.standardSeconds(20));
        roomExecutableCache.setExpiration(Duration.standardSeconds(20));
        roomParticipantSnapshotCache.setExpiration(Duration.standardSeconds(20));
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
                roomParticipants = new LinkedList<RoomParticipant>();
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
     * @return collection of {@link Recording}s for given {@code roomExecutableId}
     */
    public List<Recording> getRoomRecordings(SecurityToken securityToken, String roomExecutableId)
    {
        synchronized (roomRecordingsCache) {
            List<Recording> roomRecordings = roomRecordingsCache.get(roomExecutableId);
            if (roomRecordings == null) {
                RoomExecutable roomExecutable = getRoomExecutable(securityToken, roomExecutableId);
                String resourceId = roomExecutable.getResourceId();
                String resourceRoomId = roomExecutable.getRoomId();
                roomRecordings = new LinkedList<Recording>();
                roomRecordings.addAll(resourceControlService.listRoomRecordings(
                        securityToken, resourceId, resourceRoomId));
                roomRecordingsCache.put(roomExecutableId, roomRecordings);
            }
            return roomRecordings;
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
                    throw new IllegalArgumentException("Room participant " + roomParticipantId + " doesn't exist.");
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
     * @param roomParticipantId
     * @return {@link cz.cesnet.shongo.api.MediaData} snapshot of room participant
     */
    public MediaData getRoomParticipantSnapshot(SecurityToken securityToken, String roomExecutableId, String roomParticipantId)
    {
        String cacheId = roomExecutableId + ":" + roomParticipantId;
        synchronized (roomParticipantSnapshotCache) {
            MediaData roomParticipantSnapshot = roomParticipantSnapshotCache.get(cacheId);
            if (roomParticipantSnapshot == null) {
                RoomExecutable roomExecutable = getRoomExecutable(securityToken, roomExecutableId);
                String resourceId = roomExecutable.getResourceId();
                String resourceRoomId = roomExecutable.getRoomId();
                Set<String> roomParticipantIds = new HashSet<String>();
                roomParticipantIds.add(roomParticipantId);
                Map<String, MediaData> participantSnapshots = resourceControlService.getRoomParticipantSnapshots(
                        securityToken, resourceId, resourceRoomId, roomParticipantIds);
                roomParticipantSnapshot = participantSnapshots.get(roomParticipantId);
                roomParticipantSnapshotCache.put(cacheId, roomParticipantSnapshot);
            }
            return roomParticipantSnapshot;
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
    private RoomExecutable getRoomExecutable(SecurityToken securityToken, String roomExecutableId)
    {
        synchronized (roomExecutableCache) {
            RoomExecutable roomExecutable = roomExecutableCache.get(roomExecutableId);
            if (roomExecutable == null) {
                Executable executable = executableService.getExecutable(securityToken, roomExecutableId);
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
