package cz.cesnet.shongo.controller.booking.person;

import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.api.Controller;
import cz.cesnet.shongo.controller.authorization.Authorization;

import javax.persistence.*;

/**
 * {@link AbstractPerson} that represents a shongo-user who is defined by the shongo-user-id.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class UserPerson extends AbstractPerson
{
    /**
     * Shongo-user-id of the shongo-user.
     */
    private String userId;

    /**
     * @see UserInformation
     */
    private UserInformation userInformation;

    /**
     * Full name of the person.
     */
    private String name;

    /**
     * Organization of the person.
     */
    private String organization;

    /**
     * Email to contact the person.
     */
    private String email;

    /**
     * Phone number to contact person (by sms or by call).
     */
    private String phoneNumber;

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
    public UserPerson(String userId, UserInformation userInformation)
    {
        setUserId(userId);
        this.userInformation = userInformation;
    }

    /**
     * @return {@link #userId}
     */
    @Column(length = Controller.USER_ID_COLUMN_LENGTH)
    @Access(AccessType.FIELD)
    public String getUserId()
    {
        return userId;
    }

    /**
     * @param userId sets the {@link #userId}
     */
    public void setUserId(String userId)
    {
        Authorization.getInstance().checkUserExistence(userId);
        this.userId = userId;
    }

    /**
     * @return {@link #name}
     */
    @Column(length = AbstractComplexType.DEFAULT_COLUMN_LENGTH)
    public String getName()
    {
        return name;
    }

    /**
     * @param name sets the {@link #name}
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return {@link #organization}
     */
    @Column(length = AbstractComplexType.DEFAULT_COLUMN_LENGTH)
    public String getOrganization()
    {
        return organization;
    }

    /**
     * @param organization sets the {@link #organization}
     */
    public void setOrganization(String organization)
    {
        this.organization = organization;
    }

    /**
     * @return {@link #email}
     */
    @Column(length = AbstractComplexType.DEFAULT_COLUMN_LENGTH)
    public String getEmail()
    {
        return email;
    }

    /**
     * @param email sets the {@link #email}
     */
    public void setEmail(String email)
    {
        this.email = email;
    }

    /**
     * @return {@link #phoneNumber}
     */
    @Column(length = AbstractComplexType.DEFAULT_COLUMN_LENGTH)
    public String getPhoneNumber()
    {
        return phoneNumber;
    }

    /**
     * @param phoneNumber sets the {@link #phoneNumber}
     */
    public void setPhoneNumber(String phoneNumber)
    {
        this.phoneNumber = phoneNumber;
    }


    @Override
    public UserPerson clone() throws CloneNotSupportedException
    {
        UserPerson person = (UserPerson) super.clone();
        person.setUserId(userId);
        person.setName(name);
        person.setOrganization(organization);
        person.setEmail(email);
        person.setPhoneNumber(phoneNumber);
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
    public cz.cesnet.shongo.controller.api.AbstractPerson toApi()
    {
        cz.cesnet.shongo.controller.api.UserPerson person = new cz.cesnet.shongo.controller.api.UserPerson();
        person.setId(getId());
        person.setUserId(getUserId());
        person.setName(getName());
        person.setOrganization(getOrganization());
        person.setEmail(getEmail());
        person.setUserId(getUserId());
        return person;
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.AbstractPerson api)
    {
        cz.cesnet.shongo.controller.api.UserPerson userPersonApi = (cz.cesnet.shongo.controller.api.UserPerson) api;
        setUserId(userPersonApi.getUserId());
        setName(userPersonApi.getName());
        setOrganization(userPersonApi.getOrganization());
        setEmail(userPersonApi.getEmail());
    }

    @Override
    @Transient
    public UserInformation getInformation()
    {
        if (userInformation == null) {
            userInformation = Authorization.getInstance().getUserInformation(userId);
        }
        return userInformation;
    }


}
