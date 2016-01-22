package cz.cesnet.shongo.controller.booking.person;

import cz.cesnet.shongo.SimplePersistentObject;
import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.util.ObjectHelper;

import javax.persistence.*;

/**
 * Represents a person that can have name and who can be contacted by email or phone.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Table(name = "person")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class AbstractPerson extends SimplePersistentObject implements Cloneable
{
    /**
     * @return {@link cz.cesnet.shongo.PersonInformation} for the {@link AbstractPerson}
     */
    @Transient
    public abstract PersonInformation getInformation();

    /**
     * @return person converted to API
     */
    public abstract cz.cesnet.shongo.controller.api.AbstractPerson toApi();

    /**
     * @param api from which should be the new {@link AbstractPerson} created
     * @return new instance of {@link AbstractPerson} created from given {@code api}
     */
    public static AbstractPerson createFromApi(cz.cesnet.shongo.controller.api.AbstractPerson api)
    {
        AbstractPerson person;
        if (api instanceof cz.cesnet.shongo.controller.api.AnonymousPerson) {
            person = new AnonymousPerson();
        }
        else if (api instanceof cz.cesnet.shongo.controller.api.UserPerson) {
            person = new UserPerson();
        }
        else if (api instanceof cz.cesnet.shongo.controller.api.ForeignPerson) {
            person = new ForeignPerson();
        }
        else {
            throw new TodoImplementException(api.getClass());
        }
        person.fromApi(api);
        return person;
    }

    /**
     * Synchronize person from API
     *
     * @param api
     */
    public abstract void fromApi(cz.cesnet.shongo.controller.api.AbstractPerson api);

    @Override
    public AbstractPerson clone() throws CloneNotSupportedException
    {
        AbstractPerson person = (AbstractPerson) super.clone();
        person.setIdNull();
        return person;
    }
}
