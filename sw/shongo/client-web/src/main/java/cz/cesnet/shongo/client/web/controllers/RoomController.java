package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Recording;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.RoomUser;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.CacheProvider;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.support.MessageProvider;
import cz.cesnet.shongo.client.web.models.RoomModel;
import cz.cesnet.shongo.client.web.models.UnsupportedApiException;
import cz.cesnet.shongo.controller.ControllerReportSet;
import cz.cesnet.shongo.controller.api.Executable;
import cz.cesnet.shongo.controller.api.RoomExecutable;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.UsedRoomExecutable;
import cz.cesnet.shongo.controller.api.rpc.ExecutableService;
import cz.cesnet.shongo.controller.api.rpc.ResourceControlService;
import org.joda.time.DateTimeZone;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import java.util.*;

/**
 * Controller for managing rooms.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
public class RoomController
{
    @Resource
    private ExecutableService executableService;

    @Resource
    private ResourceControlService resourceControlService;

    @Resource
    private Cache cache;

    @Resource
    private MessageSource messageSource;

    @RequestMapping(value = ClientWebUrl.ROOM_MANAGEMENT, method = RequestMethod.GET)
    public String handleRoomManagement(
            Locale locale,
            DateTimeZone timeZone,
            SecurityToken securityToken,
            @PathVariable(value = "roomId") String executableId, Model model)
    {
        // Room executable
        Executable executable = executableService.getExecutable(securityToken, executableId);
        RoomExecutable roomExecutable;
        if (executable instanceof RoomExecutable) {
            roomExecutable = (RoomExecutable) executable;
        }
        else if (executable instanceof UsedRoomExecutable) {
            UsedRoomExecutable usedRoomExecutable = (UsedRoomExecutable) executable;
            Executable usedExecutable = executableService.getExecutable(
                    securityToken, usedRoomExecutable.getRoomExecutableId());
            if (usedExecutable instanceof RoomExecutable) {
                roomExecutable = (RoomExecutable) usedExecutable;
            }
            else {
                throw new UnsupportedApiException(usedExecutable);
            }
        }
        else {
            throw new UnsupportedApiException(executable);
        }

        // Room model
        RoomModel roomModel = new RoomModel(roomExecutable, new CacheProvider(cache, securityToken),
                new MessageProvider(messageSource, locale, timeZone), executableService);
        model.addAttribute("room", roomModel);

        // Runtime room
        if (roomModel.isStarted()) {
            String resourceId = roomExecutable.getResourceId();
            String roomId = roomExecutable.getRoomId();
            Set<Technology> technologies = roomExecutable.getTechnologies();

            try {
                Room room = resourceControlService.getRoom(securityToken, resourceId, roomId);
                model.addAttribute("roomRuntime", room);

                Collection<Recording> recordings = null;
                if (technologies.size() == 1 && technologies.contains(Technology.ADOBE_CONNECT)) {
                    recordings = resourceControlService.listRecordings(securityToken, resourceId, roomId);
                }
                model.addAttribute("roomRecordings", recordings);

                if (roomModel.isAvailable()) {
                    Collection<Map> participants = new LinkedList<Map>();
                    for (RoomUser roomUser : resourceControlService.listParticipants(securityToken, resourceId, roomId)) {
                        UserInformation userInformation = null;
                        String userId = roomUser.getUserId();
                        if (userId != null) {
                            userInformation = cache.getUserInformation(securityToken, userId);
                        }
                        Map<String, Object> participant = new HashMap<String, Object>();
                        participant.put("user", userInformation);
                        participant.put("name",
                                (userInformation != null ? userInformation.getFullName() : roomUser.getDisplayName()));
                        participants.add(participant);
                    }
                    model.addAttribute("roomParticipants", participants);
                }
            }
            catch (ControllerReportSet.DeviceCommandFailedException exception) {
                model.addAttribute("roomNotAvailable", true);
            }
        }

        // Reservation request for room
        String reservationRequestId = cache.getReservationRequestIdByExecutable(securityToken, executable);
        model.addAttribute("reservationRequestId", reservationRequestId);

        return "room";
    }
}
