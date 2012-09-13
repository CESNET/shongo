package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.controller.Scheduler;
import cz.cesnet.shongo.controller.common.Person;
import cz.cesnet.shongo.fault.EntityNotFoundException;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public abstract class EndpointSpecification extends Specification
{
    /**
     * Persons that use the endpoint to connect into video conference.
     */
    private List<Person> persons = new ArrayList<Person>();

    /**
     * Defines who should initiate the call to this endpoint ({@code null} means that the {@link Scheduler}
     * can decide it).
     */
    private CallInitiation callInitiation;

    /**
     * @return {@link #persons}
     */
    @OneToMany(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public List<Person> getPersons()
    {
        return persons;
    }

    /**
     * @param id of the {@link Person}
     * @return {@link Person} with given {@code id}
     * @throws EntityNotFoundException when the {@link Person} doesn't exist
     */
    public Person getPersonById(Long id) throws EntityNotFoundException
    {
        for (Person person : persons) {
            if (person.getId().equals(id)) {
                return person;
            }
        }
        throw new EntityNotFoundException(Person.class, id);
    }

    /**
     * @param person to be added to the {@link #persons}
     */
    public void addPerson(Person person)
    {
        persons.add(person);
    }

    /**
     * @param person to be removed from the {@link #persons}
     */
    public void removePerson(Person person)
    {
        persons.remove(person);
    }

    /**
     * @return {@link #callInitiation}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public CallInitiation getCallInitiation()
    {
        return callInitiation;
    }

    /**
     * @param callInitiation sets the {@link #callInitiation}
     */
    public void setCallInitiation(CallInitiation callInitiation)
    {
        this.callInitiation = callInitiation;
    }

    @Override
    protected void fillDescriptionMap(Map<String, String> map)
    {
        super.fillDescriptionMap(map);

        map.put("callInitiation", (callInitiation != null ? callInitiation.toString() : null));
        addCollectionToMap(map, "requestedPersons", persons);
    }
}
