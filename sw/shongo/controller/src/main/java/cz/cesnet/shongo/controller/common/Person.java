package cz.cesnet.shongo.controller.common;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.controller.api.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * Person that can be contacted.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public abstract class Person extends PersistentObject implements Cloneable
{
    /**
     * @return {@link Information} for the {@link Person}
     */
    @Transient
    public abstract Information getInformation();

    /**
     * @return person converted to API
     */
    public abstract cz.cesnet.shongo.controller.api.Person toApi();

    /**
     * @param api from which should be the new {@link Person} created
     * @return new instance of {@link Person} created from given {@code api}
     */
    public static Person createFromApi(cz.cesnet.shongo.controller.api.Person api)
    {
        Person person;
        if (api instanceof cz.cesnet.shongo.controller.api.OtherPerson) {
            person = new OtherPerson();
        }
        else if (api instanceof cz.cesnet.shongo.controller.api.UserPerson) {
            person = new UserPerson();
        }
        else {
            throw new IllegalStateException(api.getClass().getSimpleName());
        }
        person.fromApi(api);
        return person;
    }

    /**
     * Synchronize person from API
     *
     * @param api
     */
    public abstract void fromApi(cz.cesnet.shongo.controller.api.Person api);

    @Override
    public abstract Person clone();

    /**
     * Information about {@link Person}.
     */
    public static interface Information
    {
        /**
         * @return full name of the {@link Person}
         */
        public String getFullName();

        /**
         * @return root organization of the {@link Person}
         */
        public String getRootOrganization();

        /**
         * @return primary email of the {@link Person}
         */
        public String getPrimaryEmail();
    }
}
