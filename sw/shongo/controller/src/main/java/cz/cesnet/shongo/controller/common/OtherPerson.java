package cz.cesnet.shongo.controller.common;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * Person that can be contacted.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class OtherPerson extends Person implements Person.Information
{
    /**
     * User-id of the {@link cz.cesnet.shongo.controller.common.OtherPerson}.
     */
    private String userId;

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
    public OtherPerson()
    {
    }

    /**
     * Constructor.
     *
     * @param name  sets the {@link #name}
     * @param email sets the {@link #email}
     */
    public OtherPerson(String name, String email)
    {
        setName(name);
        setEmail(email);
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

    /**
     * @return {@link #name}
     */
    @Column
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
    @Column
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
    @Column
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
    @Column
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
    public OtherPerson clone()
    {
        OtherPerson person = new OtherPerson();
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
        if (!(object instanceof OtherPerson)) {
            return false;
        }
        OtherPerson person = (OtherPerson) object;

        if (email != null && person.email != null) {
            return email.equals(person.email);
        }
        if (email != null || person.email != null) {
            return false;
        }

        if (phoneNumber != null && person.phoneNumber != null) {
            return phoneNumber.equals(person.phoneNumber);
        }
        if (phoneNumber != null || person.phoneNumber != null) {
            return false;
        }

        if (organization != null && person.organization != null) {
            return organization.equals(person.organization);
        }
        if (name != null || person.name != null) {
            return false;
        }

        if (name != null && person.name != null) {
            return name.equals(person.name);
        }
        if (name != null || person.name != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        if (email != null) {
            return 31 * hash + email.hashCode();
        }
        if (phoneNumber != null) {
            return 31 * hash + phoneNumber.hashCode();
        }
        if (organization != null) {
            return 31 * hash + organization.hashCode();
        }
        if (name != null) {
            return 31 * hash + name.hashCode();
        }
        return hash;
    }

    @Override
    public cz.cesnet.shongo.controller.api.Person toApi()
    {
        cz.cesnet.shongo.controller.api.OtherPerson person = new cz.cesnet.shongo.controller.api.OtherPerson();
        person.setId(getId());
        person.setName(getName());
        person.setOrganization(getOrganization());
        person.setEmail(getEmail());
        return person;
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Person api)
    {
        cz.cesnet.shongo.controller.api.OtherPerson otherPersonApi = (cz.cesnet.shongo.controller.api.OtherPerson) api;
        setName(otherPersonApi.getName());
        setOrganization(otherPersonApi.getOrganization());
        setEmail(otherPersonApi.getEmail());
    }

    @Override
    @Transient
    public Information getInformation()
    {
        return this;
    }

    @Override
    @Transient
    public String getFullName()
    {
        return name;
    }

    @Override
    @Transient
    public String getRootOrganization()
    {
        return organization;
    }

    @Override
    @Transient
    public String getPrimaryEmail()
    {
        return email;
    }
}
