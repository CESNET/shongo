package cz.cesnet.shongo.controller.booking.person;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.api.Controller;
import cz.cesnet.shongo.controller.booking.domain.Domain;
import cz.cesnet.shongo.controller.cache.DomainCache;
import cz.cesnet.shongo.controller.domains.InterDomainAgent;

import javax.persistence.*;

/**
 * Person that can be contacted and has principal names.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
@Entity
public class ForeignPerson  extends AbstractPerson implements PersonInformation
{
    /**
     * Home domain of foreign user. MUST not be null.
     */
    private Domain domain;

    /**
     * User's ID from foreign domain.
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
    public ForeignPerson()
    {
    }

    @ManyToOne
    @Access(AccessType.FIELD)
    @JoinColumn(name = "domain_id")
    public Domain getDomain()
    {
        if (domain == null) {
            throw new IllegalArgumentException("Value domain must be set.");
        }
        return domain;
    }

    public void setDomain(Domain domain)
    {
        if (domain == null) {
            throw new IllegalArgumentException("Value domain must be set.");
        }
        this.domain = domain;
    }

    @Column(length = Controller.USER_ID_COLUMN_LENGTH)
    @Access(AccessType.FIELD)
    public String getUserId()
    {
        return userId;
    }

    public void setUserId(String userId)
    {
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
    public ForeignPerson clone() throws CloneNotSupportedException
    {
        ForeignPerson person = (ForeignPerson) super.clone();
        person.setUserId(userId);
        person.setDomain(domain);
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
        if (!(object instanceof ForeignPerson)) {
            return false;
        }
        ForeignPerson person = (ForeignPerson) object;

        if (userId != null && person.userId != null) {
            return userId.equals(person.userId);
        }
        if (domain != null && person.domain != null) {
            return domain.equals(person.domain);
        }
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
    public cz.cesnet.shongo.controller.api.ForeignPerson toApi()
    {
        cz.cesnet.shongo.controller.api.ForeignPerson person = new cz.cesnet.shongo.controller.api.ForeignPerson();
        person.setId(getId());
        person.setName(getName());
        person.setOrganization(getOrganization());
        person.setEmail(getEmail());
        return person;
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.AbstractPerson api)
    {
        cz.cesnet.shongo.controller.api.ForeignPerson foreignPersonApi =
                (cz.cesnet.shongo.controller.api.ForeignPerson) api;
        String foreignUserId = foreignPersonApi.getUserId();
        String userId = UserInformation.parseUserId(foreignUserId);
        Long domainId = UserInformation.parseDomainId(foreignUserId);
        DomainCache domainCache = InterDomainAgent.getInstance().getDomainService().getDomainCache();
        Domain domain = domainCache.getObject(domainId);

        setUserId(userId);
        setDomain(domain);
        setName(foreignPersonApi.getName());
        setOrganization(foreignPersonApi.getOrganization());
        setEmail(foreignPersonApi.getEmail());
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
