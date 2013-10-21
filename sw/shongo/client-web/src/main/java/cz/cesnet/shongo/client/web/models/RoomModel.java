package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.client.web.CacheProvider;
import cz.cesnet.shongo.client.web.support.MessageProvider;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.ExecutableListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.rpc.ExecutableService;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.util.LinkedList;
import java.util.List;

/**
 * Represents a room which can be constructed from {@link AbstractRoomExecutable}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RoomModel
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
     * List of participants of {@link AbstractRoomExecutable} and of the active {@link UsedRoomExecutable} (if it exists).
     */
    private List<Participant> participants = new LinkedList<Participant>();

    /**
     * Constructor.
     *
     * @param roomExecutable       to initialize the {@link RoomModel} from
     * @param cacheProvider        which can be used for initializing
     * @param messageProvider      sets the {@link #messageProvider}
     * @param executableService    which can be used for initializing
     * @param userSession          which can be used for initializing
     */
    public RoomModel(AbstractRoomExecutable roomExecutable, CacheProvider cacheProvider,
            MessageProvider messageProvider, ExecutableService executableService, UserSession userSession)
    {
        this.messageProvider = messageProvider;

        // Setup room
        this.id = roomExecutable.getId();
        this.slot = roomExecutable.getSlot();
        this.technology = TechnologyModel.find(roomExecutable.getTechnologies());
        this.aliases = roomExecutable.getAliases();
        for (Alias alias : roomExecutable.getAliases()) {
            if (alias.getType().equals(AliasType.ROOM_NAME)) {
                this.name = alias.getValue();
                break;
            }
        }

        // Add room participants from used executable
        if (roomExecutable instanceof UsedRoomExecutable) {
            UsedRoomExecutable usageExecutable = (UsedRoomExecutable) roomExecutable;
            AbstractRoomExecutable usedExecutable = (AbstractRoomExecutable) executableService.getExecutable(
                    cacheProvider.getSecurityToken(), usageExecutable.getRoomExecutableId());
            RoomExecutableParticipantConfiguration participants = usedExecutable.getParticipantConfiguration();
            for (AbstractParticipant participant : participants.getParticipants()) {
                this.participants.add(new Participant(usedExecutable.getId(), participant, cacheProvider));
            }
        }

        // Add room participants from executable
        for (AbstractParticipant participant : roomExecutable.getParticipantConfiguration().getParticipants()) {
            participants.add(new Participant(this.id, participant, cacheProvider));
        }

        // Room type and license count and active usage
        this.licenseCount = roomExecutable.getLicenseCount();
        if (this.licenseCount == 0) {
            this.type = RoomType.PERMANENT_ROOM;

            // Get license count from active usage
            SecurityToken securityToken = cacheProvider.getSecurityToken();
            ExecutableListRequest request = new ExecutableListRequest();
            request.setSecurityToken(securityToken);
            request.setRoomId(roomExecutable.getId());
            ListResponse<ExecutableSummary> usageSummaries = executableService.listExecutables(request);
            DateTime dateTimeNow = DateTime.now();
            for (ExecutableSummary usageSummary : usageSummaries) {
                Interval usageSlot = usageSummary.getSlot();
                if (usageSlot.contains(dateTimeNow) && usageSummary.getState().isAvailable()) {
                    UsedRoomExecutable usage = (UsedRoomExecutable) executableService.getExecutable(
                            securityToken, usageSummary.getId());
                    this.licenseCount = usage.getLicenseCount();
                    this.licenseCountUntil = usageSlot.getEnd();
                    this.usageId = usage.getId();
                    this.usageState = usage.getState();
                    RoomExecutableParticipantConfiguration participants = usage.getParticipantConfiguration();
                    for (AbstractParticipant participant : participants.getParticipants()) {
                        this.participants.add(new Participant(this.usageId, participant, cacheProvider));
                    }
                    break;
                }
            }
        }
        else if (roomExecutable instanceof UsedRoomExecutable) {
            this.type = RoomType.USED_ROOM;
        }
        else {
            this.type = RoomType.ADHOC_ROOM;
        }

        // Room state
        this.state = RoomState.fromRoomState(
                roomExecutable.getState(), roomExecutable.getLicenseCount(), usageState);
        if (!this.state.isAvailable() && userSession.isAdminMode()) {
            this.stateReport = roomExecutable.getStateReport().toString(
                    messageProvider.getLocale(), messageProvider.getTimeZone());
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
     * @return {@link #participants}
     */
    public List<Participant> getParticipants()
    {
        return participants;
    }

    /**
     * Disable modifying/deleting of dependent participants.
     */
    public void disableDependentParticipants()
    {
        for (Participant participant : participants) {
            if (!participant.roomId.equals(id)) {
                participant.setNullId();
            }
        }
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
                case ADOBE_CONNECT_URI:
                    aliasValue = alias.getValue();
                    aliasValue = aliasValue.replaceFirst("http(s)?\\://", "");
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
                    stringBuilder.append("<tr><td class=\"label\">");
                    stringBuilder.append(messageProvider.getMessage("views.room.alias.H323_E164"));
                    stringBuilder.append(":</td><td>");
                    stringBuilder.append(formatSelectable("+420" + alias.getValue()));
                    stringBuilder.append("</td></tr>");
                    stringBuilder.append("<tr><td class=\"label\">");
                    stringBuilder.append(messageProvider.getMessage("views.room.alias.H323_E164_GDS"));
                    stringBuilder.append(":</td><td>");
                    stringBuilder.append(formatSelectable("(00420)" + alias.getValue()));
                    stringBuilder.append("</td></tr>");
                    break;
                case H323_URI:
                case H323_IP:
                case SIP_URI:
                case SIP_IP:
                    stringBuilder.append("<tr><td class=\"label\">");
                    stringBuilder.append(messageProvider.getMessage("views.room.alias." + aliasType));
                    stringBuilder.append(":</td><td>");
                    stringBuilder.append(formatSelectable(alias.getValue()));
                    stringBuilder.append("</td></tr>");
                    break;
                case ADOBE_CONNECT_URI:
                    stringBuilder.append("<tr><td class=\"label\">");
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
