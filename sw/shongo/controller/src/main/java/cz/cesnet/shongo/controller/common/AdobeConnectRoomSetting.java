package cz.cesnet.shongo.controller.common;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Represents a {@link cz.cesnet.shongo.controller.common.RoomSetting} for a {@link cz.cesnet.shongo.controller.common.RoomConfiguration} which
 * supports {@link cz.cesnet.shongo.Technology#ADOBE_CONNECT}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class AdobeConnectRoomSetting extends RoomSetting
{
    /**
     * List of participants which are allowed to enter the room.
     */
    private List<String> participants = new ArrayList<String>();

    /**
     * @return {@link #participants}
     */
    @ElementCollection
    @Access(AccessType.FIELD)
    public List<String> getParticipants()
    {
        return participants;
    }

    /**
     * @param participants sets the {@link #participants}
     */
    public void setParticipants(List<String> participants)
    {
        this.participants = participants;
    }

    /**
     * @param participant to be added to the {@link #participants}
     */
    public void addParticipant(String participant)
    {
        participants.add(participant);
    }

    /**
     * @param participant to be removed from the {@link #participants}
     */
    public void removeParticipant(String participant)
    {
        participants.remove(participant);
    }

    @Override
    public RoomSetting clone()
    {
        AdobeConnectRoomSetting roomSetting = new AdobeConnectRoomSetting();
        for (String participant : participants) {
            roomSetting.addParticipant(participant);
        }
        return roomSetting;
    }

    @Override
    protected cz.cesnet.shongo.api.RoomSetting createApi()
    {
        return new cz.cesnet.shongo.api.RoomSetting.AdobeConnect();
    }

    @Override
    public void toApi(cz.cesnet.shongo.api.RoomSetting roomSettingApi)
    {
        super.toApi(roomSettingApi);

        cz.cesnet.shongo.api.RoomSetting.AdobeConnect roomSettingAdobeConnectApi =
                (cz.cesnet.shongo.api.RoomSetting.AdobeConnect) roomSettingApi;
        for (String participant : participants) {
            roomSettingAdobeConnectApi.addParticipant(participant);
        }
    }

    @Override
    public void fromApi(cz.cesnet.shongo.api.RoomSetting roomSettingApi)
    {
        super.fromApi(roomSettingApi);

        cz.cesnet.shongo.api.RoomSetting.AdobeConnect roomSettingAdobeConnectApi =
                (cz.cesnet.shongo.api.RoomSetting.AdobeConnect) roomSettingApi;

        // Create participants
        for (String participant : roomSettingAdobeConnectApi.getParticipants()) {
            if (roomSettingAdobeConnectApi.isPropertyItemMarkedAsNew(roomSettingAdobeConnectApi.PARTICIPANTS,
                    participant)) {
                addParticipant(participant);
            }
        }
        // Delete participants
        Set<String> participantsToDelete =
                roomSettingAdobeConnectApi.getPropertyItemsMarkedAsDeleted(roomSettingAdobeConnectApi.PARTICIPANTS);
        for (String participant : participantsToDelete) {
            removeParticipant(participant);
        }
    }
}
