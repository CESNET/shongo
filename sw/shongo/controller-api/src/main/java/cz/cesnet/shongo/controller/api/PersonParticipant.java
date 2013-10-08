package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;

/**
 * {@link AbstractParticipant} for {@link AbstractPerson}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PersonParticipant extends AbstractParticipant
{
    /**
     * The requested person.
     */
    private AbstractPerson person;

    /**
     * Constructor.
     */
    public PersonParticipant()
    {
    }

    /**
     * Constructor.
     *
     * @param name  sets the {@link cz.cesnet.shongo.controller.api.AnonymousPerson#name} for the {@link #PERSON}
     * @param email sets the {@link cz.cesnet.shongo.controller.api.AnonymousPerson#email} for the {@link #PERSON}
     */
    public PersonParticipant(String name, String email)
    {
        setPerson(new AnonymousPerson(name, email));
    }

    /**
     * @return {@link #PERSON}
     */
    public AbstractPerson getPerson()
    {
        return person;
    }

    /**
     * @param person sets the {@link #PERSON}
     */
    public void setPerson(AbstractPerson person)
    {
        this.person = person;
    }

    public static final String PERSON = "person";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(PERSON, person);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        person = dataMap.getComplexTypeRequired(PERSON, AbstractPerson.class);
    }
}
