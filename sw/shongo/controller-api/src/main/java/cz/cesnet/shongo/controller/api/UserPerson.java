package cz.cesnet.shongo.controller.api;

/**
* {@link Person} which is known to Shongo by user-id.
*
* @author Martin Srom <martin.srom@cesnet.cz>
*/
public class UserPerson extends Person
{
    /**
     * User-id of the person.
     */
    private String userId;

    /**
     * @return {@link #userId}
     */
    public String getUserId()
    {
        return userId;
    }

    /**
     * @param userId sets the {@link #userId}
     */
    public void setUserId(String userId)
    {
        this.userId = userId;
    }
}
