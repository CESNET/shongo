package cz.cesnet.shongo.api;

import cz.cesnet.shongo.ParticipantRole;
import jade.content.Concept;

/**
 * Represents a allowed participant for a {@link Room}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RoomParticipant extends AbstractComplexType implements Concept
{
    /**
     * User information about participant.
     */
    private UserInformation userInformation;

    /**
     * @see ParticipantRole
     */
    private ParticipantRole role;

    /**
     * Constructor.
     */
    public RoomParticipant()
    {
    }

    /**
     * Constructor.
     *
     * @param userInformation sets the {@link #userInformation}
     * @param role            sets the {@link #role}
     */
    public RoomParticipant(UserInformation userInformation, ParticipantRole role)
    {
        this.userInformation = userInformation;
        this.role = role;
    }

    /**
     * @return {@link #userInformation}
     */
    public UserInformation getUserInformation()
    {
        return userInformation;
    }

    /**
     * @param userInformation sets the {@link #userInformation}
     */
    public void setUserInformation(UserInformation userInformation)
    {
        this.userInformation = userInformation;
    }

    /**
     * @return {@link #role}
     */
    public ParticipantRole getRole()
    {
        return role;
    }

    /**
     * @param role sets the {@link #role}
     */
    public void setRole(ParticipantRole role)
    {
        this.role = role;
    }

    public static final String USER_INFORMATION = "userInformation";
    public static final String ROLE = "role";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(USER_INFORMATION, userInformation);
        dataMap.set(ROLE, role);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        userInformation = dataMap.getComplexType(USER_INFORMATION, UserInformation.class);
        role = dataMap.getEnum(ROLE, ParticipantRole.class);
    }

    @Override
    public String toString()
    {
        return String.format("Participant (userId: %s, name: %s, role: %s)",
                userInformation.getUserId(), userInformation.getFullName(), role);
    }
}
