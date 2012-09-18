package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.Technology;

import java.util.List;

/**
 * Represents a requested compartment in reservation request.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CompartmentSpecification extends Specification
{
    /**
     * Collection of {@link Specification}s for the {@link CompartmentSpecification}.
     */
    public static final String SPECIFICATIONS = "specifications";

    /**
     * @return {@link #SPECIFICATIONS}
     */
    public List<Specification> getSpecifications()
    {
        return getPropertyStorage().getCollection(SPECIFICATIONS, List.class);
    }

    /**
     * @param specifications {@link #SPECIFICATIONS}
     */
    public void setSpecifications(List<Specification> specifications)
    {
        getPropertyStorage().setCollection(SPECIFICATIONS, specifications);
    }

    /**
     * @param specification to be added to the {@link #SPECIFICATIONS}
     */
    public void addSpecification(Specification specification)
    {
        getPropertyStorage().addCollectionItem(SPECIFICATIONS, specification, List.class);
    }
}
