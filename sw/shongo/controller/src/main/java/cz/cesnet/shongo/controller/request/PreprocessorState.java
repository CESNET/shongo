package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.controller.Preprocessor;

/**
 * State of {@link AbstractReservationRequest in {@link Preprocessor}.
 */
public enum PreprocessorState
{
    /**
     * State tells that {@link AbstractReservationRequest} hasn't been preprocessed by a {@link Preprocessor}
     * or the {@link cz.cesnet.shongo.controller.request.AbstractReservationRequest} has changed.
     */
    NOT_PREPROCESSED,

    /**
     * State tells that {@link AbstractReservationRequest} has been preprocessed by a {@link Preprocessor}.
     */
    PREPROCESSED
}
