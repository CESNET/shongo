package cz.cesnet.shongo.controller.common;

import cz.cesnet.shongo.controller.Authorization;
import cz.cesnet.shongo.fault.TodoImplementException;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * Person that can be contacted.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class UserPerson extends Person
{
    /**
     * User-id of the {@link cz.cesnet.shongo.controller.common.UserPerson}.
     */
    private String userId;

    private Authorization.UserInformation userInformation;

    /**
     * Constructor.
     */
    public UserPerson()
    {
    }

    /**
     * Constructor.
     *
     * @param userId sets the {@link #userId}
     */
    public UserPerson(String userId)
    {
        setUserId(userId);
    }

    /**
     * Constructor.
     *
     * @param userId sets the {@link #userId}
     */
    public UserPerson(String userId, Authorization.UserInformation userInformation)
    {
        setUserId(userId);
        this.userInformation = userInformation;
    }

    /**
     * @return {@link #userId}
     */
    @Column
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

    @Override
    public UserPerson clone()
    {
        UserPerson person = new UserPerson();
        person.setUserId(userId);
        return person;
    }

    @Override
    public boolean equals(Object object)
    {
        if (this == object) {
            return true;
        }
        if (!(object instanceof UserPerson)) {
            return false;
        }
        UserPerson person = (UserPerson) object;

        if (userId != null && person.userId != null) {
            return userId.equals(person.userId);
        }
        if (userId != null || person.userId != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        if (userId != null) {
            return 31 * hash + userId.hashCode();
        }
        return hash;
    }

    @Override
    public cz.cesnet.shongo.controller.api.Person toApi()
    {
        cz.cesnet.shongo.controller.api.UserPerson person = new cz.cesnet.shongo.controller.api.UserPerson();
        person.setId(getId());
        person.setUserId(getUserId());
        return person;
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Person api)
    {
        cz.cesnet.shongo.controller.api.UserPerson userPersonApi = (cz.cesnet.shongo.controller.api.UserPerson) api;
        setUserId(userPersonApi.getUserId());
    }

    @Override
    @Transient
    public Information getInformation()
    {
        if (userInformation == null) {
            userInformation = Authorization.UserInformation.getInstance(userId);
        }
        return userInformation;
    }
}
