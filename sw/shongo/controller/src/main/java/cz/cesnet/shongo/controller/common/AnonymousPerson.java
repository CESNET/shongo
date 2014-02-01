package cz.cesnet.shongo.controller.common;

import cz.cesnet.shongo.PersonInformation;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * Person that can be contacted.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class AnonymousPerson extends AbstractPerson implements PersonInformation
{
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
    public AnonymousPerson()
    {
    }

    /**
     * Constructor.
     *
     * @param name  sets the {@link #name}
     * @param email sets the {@link #email}
     */
    public AnonymousPerson(String name, String email)
    {
        setName(name);
        setEmail(email);
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
    public AnonymousPerson clone()
    {
        AnonymousPerson person = new AnonymousPerson();
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
        if (!(object instanceof AnonymousPerson)) {
            return false;
        }
        AnonymousPerson person = (AnonymousPerson) object;

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
    public cz.cesnet.shongo.controller.api.AbstractPerson toApi()
    {
        cz.cesnet.shongo.controller.api.AnonymousPerson person = new cz.cesnet.shongo.controller.api.AnonymousPerson();
        person.setId(getId());
        person.setName(getName());
        person.setOrganization(getOrganization());
        person.setEmail(getEmail());
        return person;
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.AbstractPerson api)
    {
        cz.cesnet.shongo.controller.api.AnonymousPerson anonymousPersonApi =
                (cz.cesnet.shongo.controller.api.AnonymousPerson) api;
        setName(anonymousPersonApi.getName());
        setOrganization(anonymousPersonApi.getOrganization());
        setEmail(anonymousPersonApi.getEmail());
    }

    @Override
    @Transient
    public PersonInformation getInformation()
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

    @Override
    public String toString()
    {
        return getClass().getSimpleName() + " (" + getFullName() + ")";
    }
}
