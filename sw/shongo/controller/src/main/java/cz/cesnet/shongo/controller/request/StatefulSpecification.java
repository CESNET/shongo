package cz.cesnet.shongo.controller.request;

import java.util.Map;

/**
 * Should be implemented by {@link Specification}s which has {@link #getCurrentState()} based on
 * some persisted properties.
 * <p/>
 * {@link StatefulSpecification}s must be able to {@link #clone()} itself because when they are specified in a
 * {@link ReservationRequestSet} they must be duplicated for every {@link ReservationRequest} which is created
 * from the {@link ReservationRequestSet} to not share the same state.
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
