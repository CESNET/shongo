package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.api.IdentifiedComplexType;
import cz.cesnet.shongo.api.UserInformation;
import org.joda.time.DateTime;
import org.joda.time.Period;

/**
 * Represents referenced {@link cz.cesnet.shongo.api.UserInformation}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReferencedUser extends IdentifiedComplexType
{
    /**
     * {@link UserInformation} of user who is referenced
     */
    private UserInformation userInformation;

    /**
     * Number of {@link AbstractReservationRequest}s which reference the {@link #userInformation}.
     */
    private int reservationRequestCount;

    /**
     * Number of {@link Resource}s which reference the {@link #userInformation}.
     */
    private int resourceCount;

    /**
     * Number of {@link UserSettings}s which reference the {@link #userInformation}.
     */
    private int userSettingCount;

    /**
     * Number of {@link AclEntry}s which reference the {@link #userInformation}.
     */
    private int aclEntryCount;

    /**
     * Number of {@link UserPerson}s which reference the {@link #userInformation}.
     */
    private int userPersonCount;

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
     * @return {@link #reservationRequestCount}
     */
    public int getReservationRequestCount()
    {
        return reservationRequestCount;
    }

    /**
     * @param reservationRequestCount sets the {@link #reservationRequestCount}
     */
    public void setReservationRequestCount(int reservationRequestCount)
    {
        this.reservationRequestCount = reservationRequestCount;
    }

    /**
     * @return {@link #resourceCount}
     */
    public int getResourceCount()
    {
        return resourceCount;
    }

    /**
     * @param resourceCount sets the {@link #resourceCount}
     */
    public void setResourceCount(int resourceCount)
    {
        this.resourceCount = resourceCount;
    }

    /**
     * @return {@link #userSettingCount}
     */
    public int getUserSettingCount()
    {
        return userSettingCount;
    }

    /**
     * @param userSettingCount sets the {@link #userSettingCount}
     */
    public void setUserSettingCount(int userSettingCount)
    {
        this.userSettingCount = userSettingCount;
    }

    /**
     * @return {@link #aclEntryCount}
     */
    public int getAclEntryCount()
    {
        return aclEntryCount;
    }

    /**
     * @param aclEntryCount sets the {@link #aclEntryCount}
     */
    public void setAclEntryCount(int aclEntryCount)
    {
        this.aclEntryCount = aclEntryCount;
    }

    /**
     * @return {@link #userPersonCount}
     */
    public int getUserPersonCount()
    {
        return userPersonCount;
    }

    /**
     * @param userPersonCount sets the {@link #userPersonCount}
     */
    public void setUserPersonCount(int userPersonCount)
    {
        this.userPersonCount = userPersonCount;
    }

    public static final String USER_INFORMATION = "userInformation";
    public static final String RESERVATION_REQUEST_COUNT = "reservationRequestCount";
    public static final String RESOURCE_COUNT = "resourceCount";
    public static final String USER_SETTING_COUNT = "userSettingCount";
    public static final String ACL_ENTRY_COUNT = "aclEntryCount";
    public static final String USER_PERSON_COUNT = "userPersonCount";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(USER_INFORMATION, userInformation);
        dataMap.set(RESERVATION_REQUEST_COUNT, reservationRequestCount);
        dataMap.set(RESOURCE_COUNT, resourceCount);
        dataMap.set(USER_SETTING_COUNT, userSettingCount);
        dataMap.set(ACL_ENTRY_COUNT, aclEntryCount);
        dataMap.set(USER_PERSON_COUNT, userPersonCount);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        userInformation = dataMap.getComplexType(USER_INFORMATION, UserInformation.class);
        reservationRequestCount = dataMap.getInt(RESERVATION_REQUEST_COUNT, 0);
        resourceCount = dataMap.getInt(RESOURCE_COUNT, 0);
        userSettingCount = dataMap.getInt(USER_SETTING_COUNT, 0);
        aclEntryCount = dataMap.getInt(ACL_ENTRY_COUNT, 0);
        userPersonCount = dataMap.getInt(USER_PERSON_COUNT, 0);
    }
}
