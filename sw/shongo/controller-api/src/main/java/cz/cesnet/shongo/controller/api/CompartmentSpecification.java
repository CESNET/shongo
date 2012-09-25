package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.CallInitiation;

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
     * {@link CallInitiation}
     */
    public static final String CALL_INITIATION = "callInitiation";

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

    /**
     * @return {@link #CALL_INITIATION}
     */
    public CallInitiation getCallInitiation()
    {
        return getPropertyStorage().getValue(CALL_INITIATION);
    }
    /**
     * @param callInitiation sets the {@link #CALL_INITIATION}
     */
    public void setCallInitiation(CallInitiation callInitiation)
    {
        getPropertyStorage().setValue(CALL_INITIATION, callInitiation);
    }
}
