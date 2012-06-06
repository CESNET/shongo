package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.common.PersistentObject;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
     * @param requestedResource resource to be added to the list of requested resources
     */
    public void addRequestedResource(ResourceSpecification requestedResource)
    {
        this.requestedResources.add(requestedResource);
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

    /**
     * @param requestedPerson person to be added to the list of requested persons
     */
    public void addRequestedPerson(PersonRequest requestedPerson)
    {
        this.requestedPersons.add(requestedPerson);
    }

    @Override
    protected void fillDescriptionMap(Map<String, String> map)
    {
        super.fillDescriptionMap(map);

        addCollectionToMap(map, "persons", requestedPersons);
        addCollectionToMap(map, "resources", requestedResources);
    }
}
