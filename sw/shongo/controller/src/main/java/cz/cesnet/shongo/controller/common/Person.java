package cz.cesnet.shongo.controller.common;

import cz.cesnet.shongo.PersistentObject;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.util.Map;

/**
 * Person that can be contacted.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class Person extends PersistentObject
{
    /**
     * Full name of the person.
     */
    private String name;

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
    public Person()
    {
    }

    /**
     * Constructor.
     *
     * @param name  sets the {@link #name}
     * @param email sets the {@link #email}
     */
    public Person(String name, String email)
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
    public boolean equals(Object object)
    {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Person)) {
            return false;
        }
        Person person = (Person) object;

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
        if (name != null) {
            return 31 * hash + name.hashCode();
        }
        return hash;
    }

    @Override
    protected void fillDescriptionMap(Map<String, String> map)
    {
        super.fillDescriptionMap(map);

        map.put("name", getName());
        map.put("email", getEmail());
        map.put("phoneNumber", getPhoneNumber());
    }

    /**
     * @return person converted to API
     */
    public cz.cesnet.shongo.controller.api.Person toApi()
    {
        cz.cesnet.shongo.controller.api.Person person = new cz.cesnet.shongo.controller.api.Person();
        person.setId(getId().intValue());
        person.setName(getName());
        person.setEmail(getEmail());
        return person;
    }
}
