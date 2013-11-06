package cz.cesnet.shongo.controller.booking.request;

import cz.cesnet.shongo.controller.scheduler.Preprocessor;

/**
 * State of {@link AbstractReservationRequest in {@link Preprocessor}.
 */
public enum PreprocessorState
{
    /**
     * State tells that {@link AbstractReservationRequest} hasn't been preprocessed by a {@link Preprocessor}
     * or the {@link AbstractReservationRequest} has changed.
     */
    NOT_PREPROCESSED,

    /**
     * State tells that {@link AbstractReservationRequest} has been preprocessed by a {@link Preprocessor}.
     */
    PREPROCESSED
}
