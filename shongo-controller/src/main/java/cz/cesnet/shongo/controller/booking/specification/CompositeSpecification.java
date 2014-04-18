package cz.cesnet.shongo.controller.booking.specification;

import java.util.List;

/**
 * Should be implemented by {@link Specification}s which are composed from multiple other {@link Specification}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface CompositeSpecification
{
    /**
     * @return collection of {@link Specification}s from which is the {@link CompositeSpecification} composed.
     */
    public List<? extends Specification> getChildSpecifications();

    /**
     * @param specification to be added to the {@link CompositeSpecification}.
     */
    public void addChildSpecification(Specification specification);

    /**
     * @param specification to be removed from the {@link CompositeSpecification}.
     */
    public void removeChildSpecification(Specification specification);
}
