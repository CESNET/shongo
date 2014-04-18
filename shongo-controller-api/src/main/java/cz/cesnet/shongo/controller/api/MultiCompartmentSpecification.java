package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;

import java.util.LinkedList;
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
    private List<CompartmentSpecification> compartmentSpecifications = new LinkedList<CompartmentSpecification>();

    public MultiCompartmentSpecification()
    {
    }

    /**
     * @return {@link #compartmentSpecifications}
     */
    public List<CompartmentSpecification> getCompartmentSpecifications()
    {
        return compartmentSpecifications;
    }

    /**
     * @param compartmentSpecifications {@link #compartmentSpecifications}
     */
    public void setSpecifications(List<CompartmentSpecification> compartmentSpecifications)
    {
        this.compartmentSpecifications = compartmentSpecifications;
    }

    /**
     * @param compartmentSpecification to be added to the {@link #compartmentSpecifications}
     */
    public void addSpecification(CompartmentSpecification compartmentSpecification)
    {
        compartmentSpecifications.add(compartmentSpecification);
    }

    public static final String COMPARTMENT_SPECIFICATIONS = "compartmentSpecifications";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(COMPARTMENT_SPECIFICATIONS, compartmentSpecifications);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        compartmentSpecifications = dataMap.getListRequired(COMPARTMENT_SPECIFICATIONS, CompartmentSpecification.class);
    }
}
