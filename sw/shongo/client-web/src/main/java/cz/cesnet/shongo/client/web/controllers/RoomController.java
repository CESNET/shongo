package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.RoomUser;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.models.ReservationRequestModel;
import cz.cesnet.shongo.client.web.models.UnsupportedApiException;
import cz.cesnet.shongo.controller.api.Executable;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.rpc.ExecutableService;
import cz.cesnet.shongo.controller.api.rpc.ResourceControlService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;

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
    private Cache cache;

    @Resource
    private ExecutableService executableService;

    @Resource
    private ResourceControlService resourceControlService;

    @RequestMapping(value = "/{id:.+}", method = RequestMethod.GET)
    public String getRoom(
            SecurityToken securityToken,
            @PathVariable(value = "id") String executableId, Model model)
    {
        Executable executable = executableService.getExecutable(securityToken, executableId);
        Executable.ResourceRoom resourceRoom = (Executable.ResourceRoom) executable;
        if (resourceRoom == null) {
            throw new UnsupportedApiException(executable);
        }

        if (resourceRoom.getState().isAvailable()) {
            String resourceId = resourceRoom.getResourceId();
            String roomId = resourceRoom.getRoomId();
            Set<Technology> technologies = resourceRoom.getTechnologies();
            Room room = resourceControlService.getRoom(securityToken, resourceId, roomId);
            Collection<UserInformation> participants = new LinkedList<UserInformation>();
            for (RoomUser roomUser : resourceControlService.listParticipants(securityToken, resourceId, roomId)) {
                participants.add(cache.getUserInformation(securityToken, roomUser.getUserId()));
            }
            Collection<String> recordings = null;
            if (technologies.size() == 1 && technologies.contains(Technology.ADOBE_CONNECT)) {
                recordings = resourceControlService.listRecordings(securityToken, resourceId, roomId);
            }
            model.addAttribute("room", room);
            model.addAttribute("participants", participants);
            model.addAttribute("recordings", recordings);
        }
        model.addAttribute("executable", resourceRoom);
        return "room";
    }
}
