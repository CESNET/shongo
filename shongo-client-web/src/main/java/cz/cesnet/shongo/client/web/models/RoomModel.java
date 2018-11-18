package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.api.AdobeConnectRoomSetting;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.H323RoomSetting;
import cz.cesnet.shongo.api.RoomSetting;
import cz.cesnet.shongo.client.web.CacheProvider;
import cz.cesnet.shongo.client.web.support.MessageProvider;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.ExecutableListRequest;
import cz.cesnet.shongo.controller.api.request.ExecutableServiceListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.rpc.ExecutableService;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;

import java.util.List;

/**
 * Represents a room which can be constructed from {@link AbstractRoomExecutable}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RoomModel extends ParticipantConfigurationModel
{
    /**
     * {@link MessageProvider} used for formatting of {@link #aliases}.
     */
    private final MessageProvider messageProvider;

    /**
     * {@link AbstractRoomExecutable#id}
     */
    private String id;

    /**
     * @see RoomType
     */
    private RoomType type;

    /**
     * Time slot of the {@link AbstractRoomExecutable}.
     */
    private Interval slot;

    /**
     * Slot before.
     */
    private Period slotBefore;

    /**
     * Slot after.
     */
    private Period slotAfter;

    /**
     * {@link TechnologyModel} of the {@link RoomModel}.
     */
    private TechnologyModel technology;

    /**
     * Name of the {@link RoomModel}.
     */
    private String name;

    /**
     * License count of the {@link AbstractRoomExecutable} or of the active {@link UsedRoomExecutable} (if it exists).
     */
    private int licenseCount;

    /**
     * Ending date/time from time slot of the active {@link UsedRoomExecutable} (if it exists).
     */
    private DateTime licenseCountUntil;

    /**
     * List of {@link Alias}es of the {@link RoomModel}.
     */
    private List<Alias> aliases;

    /**
     * PIN.
     */
    private String pin;

    /**
     * @see RoomState
     */
    private RoomState state;

    /**
     * String description of {@link AbstractRoomExecutable#stateReport}.
     */
    private String stateReport;

    /**
     * Identifier of the active {@link UsedRoomExecutable} (if it exists).
     */
    private String usageId;

    /**
     * {@link ExecutableState} of the active {@link UsedRoomExecutable} (if it exists).
     */
    private ExecutableState usageState;

    /**
     * Specifies whether this room can be recorded.
     */
    private boolean hasRecordingService = false;

    /**
     * Specifies whether this room can have recordings.
     */
    private boolean hasRecordings = false;

    /**
     * {@link RecordingService} which can be currently used for recording.
     */
    private RecordingService recordingService;

    /**
     * Constructor.
     *
     * @param roomExecutable    to initialize the {@link RoomModel} from
     * @param cacheProvider     which can be used for initializing
     * @param messageProvider   sets the {@link #messageProvider}
     * @param executableService which can be used for initializing
     * @param userSession       which can be used for initializing
     * @param services          specifies whether {@link #recordingService} should be loaded
     */
    public RoomModel(AbstractRoomExecutable roomExecutable, CacheProvider cacheProvider,
            MessageProvider messageProvider, ExecutableService executableService, UserSession userSession,
            boolean services)
    {
        SecurityToken securityToken = cacheProvider.getSecurityToken();

        // Setup room
        this.messageProvider = messageProvider;
        this.id = roomExecutable.getId();
        this.slot = roomExecutable.getOriginalSlot();
        Interval slot = roomExecutable.getSlot();
        this.slotBefore = null;
        if (!slot.getStart().equals(this.slot.getStart())) {
            this.slotBefore = new Period(slot.getStart(), this.slot.getStart());
        }
        this.slotAfter = null;
        if (!slot.getEnd().equals(this.slot.getEnd())) {
            this.slotAfter = new Period(this.slot.getEnd(), slot.getEnd());
        }
        this.technology = TechnologyModel.find(roomExecutable.getTechnologies());
        this.aliases = roomExecutable.getAliases();
        for (Alias alias : roomExecutable.getAliases()) {
            if (alias.getType().equals(AliasType.ROOM_NAME)) {
                this.name = alias.getValue();
                break;
            }
        }
        loadRoomSettings(roomExecutable);

        // Add room participants from used executable
        if (roomExecutable instanceof UsedRoomExecutable) {
            UsedRoomExecutable usageExecutable = (UsedRoomExecutable) roomExecutable;
            AbstractRoomExecutable usedExecutable = (AbstractRoomExecutable) cacheProvider.getExecutable(
                    usageExecutable.getReusedRoomExecutableId());
            RoomExecutableParticipantConfiguration participants = usedExecutable.getParticipantConfiguration();
            for (AbstractParticipant participant : participants.getParticipants()) {
                addParticipant(new Participant(usedExecutable.getId(), participant, cacheProvider));
            }
        }

        // Add room participants from executable
        for (AbstractParticipant participant : roomExecutable.getParticipantConfiguration().getParticipants()) {
            addParticipant(new Participant(this.id, participant, cacheProvider));
        }

        // Room type and license count and active usage
        this.licenseCount = roomExecutable.getLicenseCount();
        this.hasRecordingService = roomExecutable.hasRecordingService();
        this.hasRecordings = roomExecutable.hasRecordings();
        if (this.licenseCount == 0) {
            this.type = RoomType.PERMANENT_ROOM;

            // Get license count from active usage
            ExecutableListRequest usageRequest = new ExecutableListRequest();
            usageRequest.setSecurityToken(securityToken);
            usageRequest.setRoomId(this.id);
            usageRequest.setSort(ExecutableListRequest.Sort.SLOT);
            usageRequest.setSortDescending(true);
            ListResponse<ExecutableSummary> usageSummaries = executableService.listExecutables(usageRequest);
            for (ExecutableSummary usageSummary : usageSummaries) {
                Interval usageSlot = usageSummary.getSlot();
                if (usageSummary.getState().isAvailable()) {
                    UsedRoomExecutable usage = (UsedRoomExecutable) cacheProvider.getExecutable(usageSummary.getId());
                    this.licenseCount = usage.getLicenseCount();
                    this.licenseCountUntil = usageSlot.getEnd();
                    this.usageId = usage.getId();
                    this.usageState = usage.getState();
                    RoomExecutableParticipantConfiguration participants = usage.getParticipantConfiguration();
                    for (AbstractParticipant participant : participants.getParticipants()) {
                        addParticipant(new Participant(this.usageId, participant, cacheProvider));
                    }
                    loadRoomSettings(usage);
                    if (services) {
                        this.recordingService = getRecordingService(executableService, securityToken, this.usageId);
                        this.hasRecordingService = this.recordingService != null;
                    }
                    break;
                }
            }
        }
        else {
            // Capacity or ad-hoc room
            if (roomExecutable instanceof UsedRoomExecutable) {
                this.type = RoomType.USED_ROOM;
            }
            else {
                this.type = RoomType.ADHOC_ROOM;
            }
            if (services) {
                this.recordingService = getRecordingService(executableService, securityToken, this.id);
                this.hasRecordingService = this.recordingService != null;
            }
        }

        // Room state
        this.state = RoomState.fromRoomState(
                roomExecutable.getState(), roomExecutable.getLicenseCount(), usageState);
        if (!this.state.isAvailable() && userSession.isAdministrationMode()) {
            this.stateReport = roomExecutable.getStateReport().toString(
                    messageProvider.getLocale(), messageProvider.getTimeZone());
        }
    }

    /**
     * @param roomExecutable to load {@link RoomSetting}s from
     */
    private void loadRoomSettings(AbstractRoomExecutable roomExecutable)
    {
        for (RoomSetting roomSetting : roomExecutable.getRoomSettings()) {
            if (roomSetting instanceof H323RoomSetting) {
                H323RoomSetting h323RoomSetting = (H323RoomSetting) roomSetting;
                pin = h323RoomSetting.getPin();
            }
            if (roomSetting instanceof AdobeConnectRoomSetting) {
                AdobeConnectRoomSetting adobeConnectRoomSetting = (AdobeConnectRoomSetting) roomSetting;
                pin = adobeConnectRoomSetting.getPin();
            }
        }
    }

    /**
     * @param executableService
     * @param securityToken
     * @param roomExecutableId
     * @return {@link RecordingService} for {@link AbstractRoomExecutable} with given {@code roomExecutableId}
     */
    private RecordingService getRecordingService(ExecutableService executableService, SecurityToken securityToken,
            String roomExecutableId)
    {
        ExecutableServiceListRequest request = new ExecutableServiceListRequest(securityToken);
        request.setExecutableId(roomExecutableId);
        request.addServiceClass(RecordingService.class);
        request.setCount(1);
        ListResponse<cz.cesnet.shongo.controller.api.ExecutableService> response =
                executableService.listExecutableServices(request);
        if (response.getCount() > 1) {
            throw new UnsupportedApiException("Room " + roomExecutableId + " has multiple recording services.");
        }
        if (response.getItemCount() > 0) {
            return (RecordingService) response.getItem(0);
        }
        else {
            return null;
        }
    }

    /**
     * @return {@link #id}
     */
    public String getId()
    {
        return id;
    }

    /**
     * @return {@link #type}
     */
    public RoomType getType()
    {
        return type;
    }

    /**
     * @return {@link #slot}
     */
    public Interval getSlot()
    {
        return slot;
    }

    /**
     * @return {@link #slotBefore}
     */
    public Period getSlotBefore()
    {
        return slotBefore;
    }

    /**
     * @return {@link #slotAfter}
     */
    public Period getSlotAfter()
    {
        return slotAfter;
    }

    /**
     * @return {@link #technology}
     */
    public TechnologyModel getTechnology()
    {
        return technology;
    }

    /**
     * @return {@link #name}
     */
    public String getName()
    {
        return name;
    }

    /**
     * @return {@link #licenseCount}
     */
    public int getLicenseCount()
    {
        return licenseCount;
    }

    /**
     * @return {@link #licenseCountUntil}
     */
    public DateTime getLicenseCountUntil()
    {
        return licenseCountUntil;
    }

    /**
     * @return {@link #state}
     */
    public RoomState getState()
    {
        return state;
    }

    /**
     * @return {@link #stateReport}
     */
    public String getStateReport()
    {
        return stateReport;
    }

    /**
     * @return {@link #usageId}
     */
    public String getUsageId()
    {
        return usageId;
    }

    /**
     * @return {@link #usageState}
     */
    public ExecutableState getUsageState()
    {
        return usageState;
    }

    /**
     * @return true whether this room slot is whole in history,
     *         false otherwise
     */
    public boolean isDeprecated()
    {
        return slot.getEnd().isBeforeNow();
    }

    /**
     * @return {@link #state#isAvailable()}
     */
    public boolean isAvailable()
    {
        return state.isAvailable();
    }

    /**
     * @return {@link #state#isStarted()}
     */
    public boolean isStarted()
    {
        return state.isStarted();
    }

    /**
     * @return short formatted {@link #aliases}
     */
    public String getAliases()
    {
        return formatAliases(aliases, isAvailable());
    }

    /**
     * @return formatted description of {@link #aliases}
     */
    public String getAliasesDescription()
    {
        return formatAliasesDescription(aliases, isAvailable(), messageProvider);
    }

    /**
     * @return {@link #pin}
     */
    public String getPin()
    {
        return pin;
    }

    /**
     * Disable modifying/deleting of dependent participants.
     */
    public void disableDependentParticipants()
    {
        for (ParticipantModel participantModel : participants) {
            Participant participant = (Participant) participantModel;
            if (!participant.roomId.equals(id)) {
                participant.setNullId();
            }
        }
    }

    /**
     * @return {@link #recordingService}
     */
    public RecordingService getRecordingService()
    {
        return recordingService;
    }

    /**
     * @return {@link #hasRecordingService}
     */
    public boolean hasRecordingService()
    {
        return hasRecordingService;
    }

    /**
     * @return {@link #hasRecordings}
     */
    public boolean hasRecordings()
    {
        return hasRecordings;
    }

    /**
     * @param text to be formatted
     * @return formatted given {@code text} to be better selectable by triple click
     */
    private static String formatSelectable(String text)
    {
        return "<span style=\"float:left\">" + text + "</span>";
    }

    /**
     * @param aliases     to be formatted
     * @param isAvailable specifies whether {@code aliases} are available now
     * @return short formatted {@code aliases}
     */
    public static String formatAliases(List<Alias> aliases, boolean isAvailable)
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<span class=\"aliases");
        if (!isAvailable) {
            stringBuilder.append(" not-available");
        }
        stringBuilder.append("\">");
        int index = 0;
        for (Alias alias : aliases) {
            AliasType aliasType = alias.getType();
            String aliasValue = null;
            switch (aliasType) {
                case H323_E164:
                    aliasValue = alias.getValue();
                    break;
                case FREEPBX_CONFERENCE_NUMBER:
                    aliasValue = alias.getValue();
                    break;
                case ADOBE_CONNECT_URI:
                    aliasValue = alias.getValue();
//                    aliasValue = aliasValue.replaceFirst("http(s)?\\://", "");
                    if (isAvailable) {
                        StringBuilder aliasValueBuilder = new StringBuilder();
                        aliasValueBuilder.append("<a class=\"nowrap\" href=\"");
                        aliasValueBuilder.append(alias.getValue());
                        aliasValueBuilder.append("\" target=\"_blank\">");
                        aliasValueBuilder.append(aliasValue);
                        aliasValueBuilder.append("</a>");
                        aliasValue = aliasValueBuilder.toString();
                    }
                    break;
            }
            if (aliasValue == null) {
                continue;
            }
            if (index > 0) {
                stringBuilder.append(",&nbsp;");
            }
            stringBuilder.append(aliasValue);
            index++;
        }
        stringBuilder.append("</span>");
        return stringBuilder.toString();
    }

    /**
     * @param aliases         to be formatted
     * @param isAvailable     specifies whether {@code aliases} are available now
     * @param messageProvider to be used for formatting messages
     * @return formatted description of {@code aliases}
     */
    public static String formatAliasesDescription(List<Alias> aliases, boolean isAvailable,
            MessageProvider messageProvider)
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<table class=\"aliases");
        if (!isAvailable) {
            stringBuilder.append(" not-available");
        }
        stringBuilder.append("\">");
        for (Alias alias : aliases) {
            AliasType aliasType = alias.getType();
            switch (aliasType) {
                case H323_E164:
/*                    stringBuilder.append("<tr><td class=\"title\">");
                    stringBuilder.append(messageProvider.getMessage("views.room.alias.H323_E164"));
                    stringBuilder.append(":</td><td>");
                    stringBuilder.append(formatSelectable("+420" + alias.getValue()));
                    stringBuilder.append("</td></tr>");*/
                    stringBuilder.append("<tr><td class=\"title\">");
                    stringBuilder.append(messageProvider.getMessage("views.room.alias.H323_SIP_PHONE"));
                    stringBuilder.append(":</td><td>");
                    stringBuilder.append(formatSelectable("(00420)" + alias.getValue()));
                    stringBuilder.append("</td></tr>");
                    break;
                case H323_URI:
                case H323_IP:
                case SIP_URI:
                case SIP_IP:
                    stringBuilder.append("<tr><td class=\"title\">");
                    stringBuilder.append(messageProvider.getMessage("views.room.alias." + aliasType));
                    stringBuilder.append(":</td><td>");
                    stringBuilder.append(formatSelectable(alias.getValue()));
                    stringBuilder.append("</td></tr>");
                    break;
                case ADOBE_CONNECT_URI:
                    stringBuilder.append("<tr><td class=\"title\">");
                    stringBuilder.append(messageProvider.getMessage("views.room.alias." + aliasType));
                    stringBuilder.append(":</td><td>");
                    if (isAvailable) {
                        stringBuilder.append("<a class=\"nowrap\" href=\"");
                        stringBuilder.append(alias.getValue());
                        stringBuilder.append("\" target=\"_blank\">");
                        stringBuilder.append(alias.getValue());
                        stringBuilder.append("</a>");
                    }
                    else {
                        stringBuilder.append(alias.getValue());
                    }
                    stringBuilder.append("</td></tr>");
                    break;
                case CS_DIAL_STRING:
                    stringBuilder.append("<tr><td class=\"title\">");
                    stringBuilder.append(messageProvider.getMessage("views.room.alias." + aliasType));
                    stringBuilder.append(":</td><td>");
                    stringBuilder.append(formatSelectable(alias.getValue()));
                    stringBuilder.append("</td></tr>");
                    break;
                case FREEPBX_CONFERENCE_NUMBER:
                    stringBuilder.append("<tr><td class=\"title\">");
                    stringBuilder.append(messageProvider.getMessage("views.room.alias." + aliasType));
                    stringBuilder.append(":</td><td>");
                    stringBuilder.append(formatSelectable("+420" + alias.getValue()));
                    stringBuilder.append("</td></tr>");
                    break;
                case SKYPE_URI:
                    if (!alias.getValue().contains("@cesnet.cz")) {
                        stringBuilder.append("<tr><td class=\"title\">");
                        stringBuilder.append(messageProvider.getMessage("views.room.alias." + aliasType));
                        stringBuilder.append(":</td><td>");
                        stringBuilder.append(alias.getValue());
                        stringBuilder.append("</td></tr>");
                    }
                    break;
                case WEB_CLIENT_URI:
                    stringBuilder.append("<tr><td class=\"title\">");
                    stringBuilder.append(messageProvider.getMessage("views.room.alias." + aliasType));
                    stringBuilder.append(":</td><td>");
                    stringBuilder.append(formatSelectable(alias.getValue()));
                    stringBuilder.append("</td></tr>");
            }
        }
        stringBuilder.append("</table>");
        if (!isAvailable) {
            stringBuilder.append("<span class=\"aliases not-available\">");
            stringBuilder.append(messageProvider.getMessage("views.room.notAvailable"));
            stringBuilder.append("</span>");
        }
        return stringBuilder.toString();
    }

    /**
     * Represents a participant in {@link RoomModel}.
     */
    public class Participant extends ParticipantModel
    {
        /**
         * Identifier of {@link AbstractRoomExecutable} to which the {@link Participant} belongs.
         * {@link RoomExecutable} can have a {@link UsedRoomExecutable} and thus the {@link RoomModel} should show
         * participants for both of them and we need to know to which of the {@link AbstractRoomExecutable}
         * the participant belongs.
         */
        private String roomId;

        /**
         * Constructor.
         *
         * @param roomId sets the {@link #roomId}
         * @see ParticipantModel#ParticipantModel
         */
        public Participant(String roomId, AbstractParticipant participant, CacheProvider cacheProvider)
        {
            super(participant, cacheProvider);
            this.roomId = roomId;
            if (this.roomId == null) {
                this.id = null;
            }
        }

        /**
         * @return {@link #roomId}
         */
        public String getRoomId()
        {
            return roomId;
        }

        /**
         * @param roomId sets {@link #roomId}
         */
        public void setRoomId(String roomId)
        {
            this.roomId = roomId;
        }

        @Override
        public String getDescription()
        {
            if (roomId.equals(RoomModel.this.id)) {
                return messageProvider.getMessage("views.participant.description.persistent");
            }
            else {
                return messageProvider.getMessage("views.participant.description.capacity");
            }
        }
    }
}
