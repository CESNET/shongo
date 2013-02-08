package cz.cesnet.shongo.controller.api;

import java.util.List;

/**
 * Represents a group of {@link CompartmentSpecification}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class MultiCompartmentSpecification extends Specification
{
    /**
     * Collection of {@link cz.cesnet.shongo.controller.api.CompartmentSpecification}s.
     */
    public static final String SPECIFICATIONS = "specifications";

    /**
     * @return {@link #SPECIFICATIONS}
     */
    public List<CompartmentSpecification> getSpecifications()
    {
        return getPropertyStorage().getCollection(SPECIFICATIONS, List.class);
    }

    /**
     * @param specifications {@link #SPECIFICATIONS}
     */
    public void setSpecifications(List<CompartmentSpecification> specifications)
    {
        getPropertyStorage().setCollection(SPECIFICATIONS, specifications);
    }

    /**
     * @param specification to be added to the {@link #SPECIFICATIONS}
     */
    public void addSpecification(CompartmentSpecification specification)
    {
        getPropertyStorage().addCollectionItem(SPECIFICATIONS, specification, List.class);
    }
}
