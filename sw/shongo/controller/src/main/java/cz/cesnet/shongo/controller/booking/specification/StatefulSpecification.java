package cz.cesnet.shongo.controller.booking.specification;

/**
 * Should be implemented by {@link Specification}s which has {@link #getCurrentState()} based on
 * some persisted properties.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface StatefulSpecification
{
    /**
     * @return current {@link State} of the {@link StatefulSpecification}
     */
    public State getCurrentState();

    /**
     * State of {@link StatefulSpecification}s.
     */
    public static enum State
    {
        /**
         * {@link StatefulSpecification} is ready for scheduling.
         */
        READY,

        /**
         * {@link StatefulSpecification} is not ready for scheduling.
         */
        NOT_READY,

        /**
         * {@link StatefulSpecification} should be skipped from scheduling.
         */
        SKIP
    }
}
