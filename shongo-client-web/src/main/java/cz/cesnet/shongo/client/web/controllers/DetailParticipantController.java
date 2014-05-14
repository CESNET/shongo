package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.CacheProvider;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.models.*;
import cz.cesnet.shongo.client.web.support.MessageProvider;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.*;

/**
 * Controller for managing {@link cz.cesnet.shongo.client.web.models.ParticipantModel}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
@SessionAttributes({DetailParticipantController.PARTICIPANT_ATTRIBUTE})
public class DetailParticipantController extends AbstractDetailController
{
    private static Logger logger = LoggerFactory.getLogger(DetailParticipantController.class);

    protected static final String PARTICIPANT_ATTRIBUTE = "participant";

    @RequestMapping(value = ClientWebUrl.DETAIL_PARTICIPANTS_TAB, method = RequestMethod.GET)
    public ModelAndView handleParticipantsTab(
            SecurityToken securityToken,
            UserSession userSession,
            @PathVariable(value = "objectId") String objectId)
    {
        String executableId = getExecutableId(securityToken, objectId);
        AbstractRoomExecutable roomExecutable = getRoomExecutable(securityToken, executableId);
        RoomState roomState = RoomState.fromRoomState(roomExecutable.getState());
        ModelAndView modelAndView = new ModelAndView("detailParticipants");
        modelAndView.addObject("isStopped", roomState.equals(RoomState.STOPPED));
        modelAndView.addObject("type", RoomType.fromRoomExecutable(roomExecutable));
        modelAndView.addObject("technology", TechnologyModel.find(roomExecutable.getTechnologies()));
        return modelAndView;
    }

    @RequestMapping(value = ClientWebUrl.DETAIL_PARTICIPANTS_DATA, method = RequestMethod.GET)
    @ResponseBody
    public Map handleParticipantData(
            SecurityToken securityToken,
            Locale locale,
            @PathVariable(value = "objectId") String objectId,
            @RequestParam(value = "start", required = false) Integer start,
            @RequestParam(value = "count", required = false) Integer count)
    {
        final CacheProvider cacheProvider = new CacheProvider(cache, securityToken);


        // Get room executable
        String executableId = getExecutableId(securityToken, objectId);
        AbstractRoomExecutable roomExecutable = getRoomExecutable(securityToken, executableId);

        List<AbstractParticipant> participants = new LinkedList<AbstractParticipant>();

        // Add reused room participants as read-only
        if (roomExecutable instanceof UsedRoomExecutable) {
            UsedRoomExecutable usedRoomExecutable = (UsedRoomExecutable) roomExecutable;
            String reusedRoomExecutableId = usedRoomExecutable.getReusedRoomExecutableId();
            RoomExecutable reusedRoomExecutable =
                    (RoomExecutable) getRoomExecutable(securityToken, reusedRoomExecutableId);
            List<AbstractParticipant> reusedRoomParticipants =
                    reusedRoomExecutable.getParticipantConfiguration().getParticipants();
            Collections.sort(reusedRoomParticipants, new Comparator<AbstractParticipant>()
            {
                @Override
                public int compare(AbstractParticipant o1, AbstractParticipant o2)
                {
                    return Integer.valueOf(o1.getId()).compareTo(Integer.valueOf(o2.getId()));
                }
            });
            for (AbstractParticipant participant : reusedRoomParticipants) {
                participant.setId((String) null);
                participants.add(participant);
            }
        }

        // Add room participants
        List<AbstractParticipant> roomParticipants = roomExecutable.getParticipantConfiguration().getParticipants();
        Collections.sort(roomParticipants, new Comparator<AbstractParticipant>()
        {
            @Override
            public int compare(AbstractParticipant o1, AbstractParticipant o2)
            {
                return Integer.valueOf(o1.getId()).compareTo(Integer.valueOf(o2.getId()));
            }
        });
        participants.addAll(roomParticipants);

        ListResponse<AbstractParticipant> response = ListResponse.fromRequest(start, count, participants);
        List<Map> items = new LinkedList<Map>();
        for (AbstractParticipant participant : response.getItems()) {
            ParticipantModel participantModel = new ParticipantModel(participant, cacheProvider);
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("id", participantModel.getId());
            item.put("type", participantModel.getType());
            item.put("name", participantModel.getName());
            item.put("email", participantModel.getEmail());
            UserInformation userInformation = participantModel.getUser();
            if (userInformation != null) {
                item.put("organization", userInformation.getOrganization());
            }
            item.put("role", messageSource.getMessage(
                    "views.participant.role." + participantModel.getRole(), null, locale));
            items.add(item);
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("start", response.getStart());
        data.put("count", response.getCount());
        data.put("items", items);
        return data;
    }

    /**
     * Show form for adding new participant for ad-hoc/permanent room.
     */
    @RequestMapping(value = ClientWebUrl.DETAIL_PARTICIPANT_CREATE, method = RequestMethod.GET)
    public ModelAndView handleParticipantCreate(
            SecurityToken securityToken,
            @PathVariable("objectId") String objectId)
    {
        AbstractRoomExecutable roomExecutable = getRoomExecutable(securityToken, objectId);
        ModelAndView modelAndView = new ModelAndView("participant");
        modelAndView.addObject("technology", TechnologyModel.find(roomExecutable.getTechnologies()));
        modelAndView.addObject(PARTICIPANT_ATTRIBUTE, new ParticipantModel(new CacheProvider(cache, securityToken)));
        return modelAndView;
    }

    /**
     * Store new {@code participant} to reservation request.
     */
    @RequestMapping(value = ClientWebUrl.DETAIL_PARTICIPANT_CREATE, method = RequestMethod.POST)
    public Object handleParticipantCreateProcess(
            SecurityToken securityToken,
            @PathVariable("objectId") String objectId,
            @ModelAttribute(PARTICIPANT_ATTRIBUTE) ParticipantModel participant,
            BindingResult bindingResult)
    {
        String executableId = getExecutableId(securityToken, objectId);
        AbstractRoomExecutable roomExecutable = getRoomExecutable(securityToken, executableId);
        participant.validate(bindingResult);
        if (bindingResult.hasErrors()) {
            CommonModel.logValidationErrors(logger, bindingResult, securityToken);
            ModelAndView modelAndView = new ModelAndView("participant");
            modelAndView.addObject("technology", TechnologyModel.find(roomExecutable.getTechnologies()));
            modelAndView.addObject("technology", TechnologyModel.find(roomExecutable.getTechnologies()));
            return modelAndView;
        }
        CacheProvider cacheProvider = new CacheProvider(cache, securityToken);
        RoomExecutableParticipantConfiguration participantConfiguration = roomExecutable.getParticipantConfiguration();

        // Initialize model from API
        ParticipantConfigurationModel participantConfigurationModel = new ParticipantConfigurationModel();
        for (AbstractParticipant existingParticipant : participantConfiguration.getParticipants()) {
            participantConfigurationModel.addParticipant(new ParticipantModel(existingParticipant, cacheProvider));
        }
        // Modify model
        participantConfigurationModel.addParticipant(participant);
        // Initialize API from model
        participantConfiguration.clearParticipants();
        for (ParticipantModel participantModel : participantConfigurationModel.getParticipants()) {
            participantConfiguration.addParticipant(participantModel.toApi());
        }
        executableService.modifyExecutableConfiguration(securityToken, executableId, participantConfiguration);
        cache.clearExecutable(executableId);
        return "redirect:" + ClientWebUrl.format(ClientWebUrl.DETAIL_PARTICIPANTS_VIEW, objectId);
    }

    /**
     * Show form for modifying existing participant for ad-hoc/permanent room.
     */
    @RequestMapping(value = ClientWebUrl.DETAIL_PARTICIPANT_MODIFY, method = RequestMethod.GET)
    public ModelAndView handleParticipantModify(
            SecurityToken securityToken,
            @PathVariable("objectId") String objectId,
            @PathVariable("participantId") String participantId)
    {
        AbstractRoomExecutable roomExecutable = getRoomExecutable(securityToken, objectId);
        RoomExecutableParticipantConfiguration participantConfiguration = roomExecutable.getParticipantConfiguration();
        ParticipantModel participant = getParticipant(participantConfiguration, participantId, securityToken);
        ModelAndView modelAndView = new ModelAndView("participant");
        modelAndView.addObject("technology", TechnologyModel.find(roomExecutable.getTechnologies()));
        modelAndView.addObject(PARTICIPANT_ATTRIBUTE, participant);
        return modelAndView;
    }

    /**
     * Store changes for existing {@code participant} to reservation request.
     */
    @RequestMapping(value = ClientWebUrl.DETAIL_PARTICIPANT_MODIFY, method = RequestMethod.POST)
    public Object handleParticipantModifyProcess(
            SecurityToken securityToken,
            @PathVariable("objectId") String objectId,
            @PathVariable("participantId") String participantId,
            @ModelAttribute(PARTICIPANT_ATTRIBUTE) ParticipantModel participant,
            BindingResult bindingResult)
    {
        String executableId = getExecutableId(securityToken, objectId);
        AbstractRoomExecutable roomExecutable = getRoomExecutable(securityToken, executableId);
        participant.validate(bindingResult);
        if (bindingResult.hasErrors()) {
            CommonModel.logValidationErrors(logger, bindingResult, securityToken);
            ModelAndView modelAndView = new ModelAndView("participant");
            modelAndView.addObject("technology", TechnologyModel.find(roomExecutable.getTechnologies()));
            return modelAndView;
        }
        CacheProvider cacheProvider = new CacheProvider(cache, securityToken);
        RoomExecutableParticipantConfiguration participantConfiguration = roomExecutable.getParticipantConfiguration();

        // Initialize model from API
        ParticipantConfigurationModel participantConfigurationModel = new ParticipantConfigurationModel();
        for (AbstractParticipant existingParticipant : participantConfiguration.getParticipants()) {
            participantConfigurationModel.addParticipant(new ParticipantModel(existingParticipant, cacheProvider));
        }
        // Modify model
        ParticipantModel oldParticipant = getParticipant(participantConfiguration, participantId, securityToken);
        participantConfigurationModel.removeParticipant(oldParticipant);
        participantConfigurationModel.addParticipant(participant);
        // Initialize API from model
        participantConfiguration.clearParticipants();
        for (ParticipantModel participantModel : participantConfigurationModel.getParticipants()) {
            participantConfiguration.addParticipant(participantModel.toApi());
        }
        executableService.modifyExecutableConfiguration(securityToken, executableId, participantConfiguration);
        cache.clearExecutable(executableId);
        return "redirect:" + ClientWebUrl.format(ClientWebUrl.DETAIL_PARTICIPANTS_VIEW, objectId);
    }

    /**
     * Delete existing {@code participant} from reservation request.
     */
    @RequestMapping(value = ClientWebUrl.DETAIL_PARTICIPANT_DELETE, method = RequestMethod.GET)
    public String handleParticipantDelete(
            SecurityToken securityToken,
            @PathVariable("objectId") String objectId,
            @PathVariable("participantId") String participantId)
    {
        String executableId = getExecutableId(securityToken, objectId);
        AbstractRoomExecutable roomExecutable = getRoomExecutable(securityToken, executableId);
        RoomExecutableParticipantConfiguration participantConfiguration = roomExecutable.getParticipantConfiguration();
        ParticipantModel oldParticipant = getParticipant(participantConfiguration, participantId, securityToken);
        participantConfiguration.removeParticipantById(oldParticipant.getId());
        executableService.modifyExecutableConfiguration(securityToken, executableId, participantConfiguration);
        cache.clearExecutable(executableId);
        return "redirect:" + ClientWebUrl.format(ClientWebUrl.DETAIL_PARTICIPANTS_VIEW, objectId);
    }

    protected ParticipantModel getParticipant(RoomExecutableParticipantConfiguration participantConfiguration,
            String participantId, SecurityToken securityToken)
    {
        AbstractParticipant participant = participantConfiguration.getParticipant(participantId);
        if (participant == null) {
            throw new IllegalArgumentException("Participant " + participantId + " doesn't exist.");
        }
        return new ParticipantModel(participant, new CacheProvider(cache, securityToken));
    }
}
