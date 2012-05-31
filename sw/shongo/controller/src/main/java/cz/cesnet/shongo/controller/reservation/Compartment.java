package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.controller.common.PersistentObject;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a group of requested resources and/or persons.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Compartment extends PersistentObject
{
    /**
     * List of specification for resources which are requested to participate in compartment.
     */
    private List<ResourceSpecification> requestedResources = new ArrayList<ResourceSpecification>();

    /**
     * List of persons which are requested to participate in compartment.
     */
    private List<PersonRequest> requestedPersons = new ArrayList<PersonRequest>();

    /**
     * @return {@link #requestedResources}
     */
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "compartment")
    public List<ResourceSpecification> getRequestedResources()
    {
        return Collections.unmodifiableList(requestedResources);
    }

    /**
     * @param requestedResources sets the {@link #requestedResources}
     */
    private void setRequestedResources(List<ResourceSpecification> requestedResources)
    {
        this.requestedResources = requestedResources;
    }

    /**
     * @return {@link #requestedPersons}
     */
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "compartment")
    public List<PersonRequest> getRequestedPersons()
    {
        return Collections.unmodifiableList(requestedPersons);
    }

    /**
     * @param requestedPersons sets the {@link #requestedPersons}
     */
    private void setRequestedPersons(List<PersonRequest> requestedPersons)
    {
        this.requestedPersons = requestedPersons;
    }
}
