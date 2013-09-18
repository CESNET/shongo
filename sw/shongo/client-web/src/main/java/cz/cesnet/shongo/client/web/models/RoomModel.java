package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.client.web.CacheProvider;
import cz.cesnet.shongo.client.web.support.MessageProvider;
import cz.cesnet.shongo.controller.api.AbstractRoomExecutable;
import cz.cesnet.shongo.controller.api.ExecutableState;
import cz.cesnet.shongo.controller.api.ExecutableSummary;
import cz.cesnet.shongo.controller.api.request.ExecutableListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.rpc.ExecutableService;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.util.List;

/**
 * Represents a room.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RoomModel
{
    private final MessageProvider messageProvider;

    private String id;

    private String reservationRequestId;

    private boolean permanentRoom;

    private Interval slot;

    private TechnologyModel technology;

    private String name;

    private int licenseCount;

    private DateTime licenseCountUntil;

    private List<Alias> aliases;

    private RoomState state;

    private String stateReport;

    private ExecutableState usageState;

    public RoomModel(AbstractRoomExecutable roomExecutable, String reservationRequestId, CacheProvider cacheProvider,
            MessageProvider messageProvider, ExecutableService executableService, UserSession userSession)
    {
        this.messageProvider = messageProvider;

        this.id = roomExecutable.getId();
        this.reservationRequestId = reservationRequestId;
        this.slot = roomExecutable.getSlot();
        this.technology = TechnologyModel.find(roomExecutable.getTechnologies());
        this.aliases = roomExecutable.getAliases();
        for (Alias alias : roomExecutable.getAliases()) {
            if (alias.getType().equals(AliasType.ROOM_NAME)) {
                this.name = alias.getValue();
                break;
            }
        }
        this.licenseCount = roomExecutable.getLicenseCount();

        if (this.licenseCount == 0) {
            permanentRoom = true;

            // Get license count from active usage
            ExecutableListRequest request = new ExecutableListRequest();
            request.setSecurityToken(cacheProvider.getSecurityToken());
            request.setRoomId(roomExecutable.getId());
            ListResponse<ExecutableSummary> executableSummaries = executableService.listExecutables(request);
            DateTime dateTimeNow = DateTime.now();
            for (ExecutableSummary executableSummary : executableSummaries) {
                Interval executableSummarySlot = executableSummary.getSlot();
                if (executableSummarySlot.contains(dateTimeNow) && executableSummary.getState().isAvailable()) {
                    licenseCount = executableSummary.getRoomLicenseCount();
                    usageState = executableSummary.getState();
                    licenseCountUntil = executableSummarySlot.getEnd();
                    break;
                }
            }
        }

        this.state = RoomState.fromRoomState(
                roomExecutable.getState(), roomExecutable.getLicenseCount(), usageState);
        if (!this.state.isAvailable() && userSession.isAdmin()) {
            this.stateReport = roomExecutable.getStateReport().toString(messageProvider.getLocale(), messageProvider.getTimeZone());
        }
    }

    public RoomModel(AbstractRoomExecutable roomExecutable, CacheProvider cacheProvider, MessageProvider messageProvider,
            ExecutableService executableService, UserSession userSession)
    {
        this(roomExecutable, cacheProvider.getReservationRequestIdByExecutable(roomExecutable),
                cacheProvider, messageProvider, executableService, userSession);
    }

    public String getId()
    {
        return id;
    }

    public String getReservationRequestId()
    {
        return reservationRequestId;
    }

    public boolean isPermanentRoom()
    {
        return permanentRoom;
    }

    public Interval getSlot()
    {
        return slot;
    }

    public TechnologyModel getTechnology()
    {
        return technology;
    }

    public String getName()
    {
        return name;
    }

    public int getLicenseCount()
    {
        return licenseCount;
    }

    public DateTime getLicenseCountUntil()
    {
        return licenseCountUntil;
    }

    public RoomState getState()
    {
        return state;
    }

    public String getStateReport()
    {
        return stateReport;
    }

    public ExecutableState getUsageState()
    {
        return usageState;
    }

    public boolean isAvailable()
    {
        return state.isAvailable();
    }

    public boolean isStarted()
    {
        return state.isStarted();
    }

    public String getAliases()
    {
        return formatAliases(aliases, isAvailable());
    }

    public String getAliasesDescription()
    {
        return formatAliasesDescription(aliases, isAvailable(), messageProvider);
    }

    /**
     * @param text
     * @return formatted given {@code text} to be better selectable by triple click
     */
    private static String formatSelectable(String text)
    {
        return "<span style=\"float:left\">" + text + "</span>";
    }

    /**
     * @param aliases
     * @param isAvailable
     * @return formatted aliases
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
     * @param aliases
     * @param isAvailable
     * @param messageProvider
     * @return formatted description of aliases
     */
    public static String formatAliasesDescription(List<Alias> aliases, boolean isAvailable, MessageProvider messageProvider)
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

}
