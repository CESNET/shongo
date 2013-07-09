package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.RoomUser;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.models.ReservationRequestModel;
import cz.cesnet.shongo.client.web.models.UnsupportedApiException;
import cz.cesnet.shongo.controller.ControllerReportSet;
import cz.cesnet.shongo.controller.api.AbstractReservationRequest;
import cz.cesnet.shongo.controller.api.Executable;
import cz.cesnet.shongo.controller.api.Reservation;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.rpc.ExecutableService;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import cz.cesnet.shongo.controller.api.rpc.ResourceControlService;
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
@RequestMapping("/room")
public class RoomController
{
    @Resource
    private ReservationService reservationService;

    @Resource
    private ExecutableService executableService;

    @Resource
    private ResourceControlService resourceControlService;

    @Resource
    private Cache cache;

    @Resource
    private MessageSource messageSource;

    @RequestMapping(value = "/{id:.+}", method = RequestMethod.GET)
    public String getRoom(
            Locale locale,
            SecurityToken securityToken,
            @PathVariable(value = "id") String executableId, Model model)
    {
        Executable executable = executableService.getExecutable(securityToken, executableId);
        String reservationRequestId = cache.getReservationRequestIdByReservation(securityToken, executable);

        Executable.ResourceRoom resourceRoom = (Executable.ResourceRoom) executable;
        if (resourceRoom == null) {
            throw new UnsupportedApiException(executable);
        }

        List<Alias> aliases = resourceRoom.getAliases();
        Executable.State executableState = resourceRoom.getState();
        model.addAttribute("roomAliases", ReservationRequestModel.formatAliases(aliases, executableState));
        model.addAttribute("roomAliasesDescription",
                ReservationRequestModel.formatAliasesDescription(aliases, executableState, locale, messageSource));

        if (resourceRoom.getState().isAvailable()) {
            String resourceId = resourceRoom.getResourceId();
            String roomId = resourceRoom.getRoomId();
            Set<Technology> technologies = resourceRoom.getTechnologies();

            try {
                Room room = resourceControlService.getRoom(securityToken, resourceId, roomId);
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
                Collection<String> recordings = null;
                if (technologies.size() == 1 && technologies.contains(Technology.ADOBE_CONNECT)) {
                    recordings = resourceControlService.listRecordings(securityToken, resourceId, roomId);
                }
                model.addAttribute("room", room);
                model.addAttribute("participants", participants);
                model.addAttribute("recordings", recordings);
            }
            catch (ControllerReportSet.DeviceCommandFailedException exception) {
                model.addAttribute("notAvailable", true);
            }
        }
        model.addAttribute("executable", resourceRoom);
        model.addAttribute("reservationRequestId", reservationRequestId);
        return "room";
    }
}
