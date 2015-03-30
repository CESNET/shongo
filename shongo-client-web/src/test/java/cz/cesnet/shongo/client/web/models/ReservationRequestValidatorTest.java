package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.controller.api.PeriodicDateTimeSlot;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.Period;
import org.junit.Test;
import org.springframework.validation.Errors;

/**
 * Created by opicak on 30.3.2015.
 */
public class ReservationRequestValidatorTest {
    @Test
    public void validatePeriodicSlotsStartTest()
    {
        ReservationRequestModel reservationRequestModel = new ReservationRequestModel(null);
        reservationRequestModel.setStart(new LocalTime("10:00"));
        reservationRequestModel.setStartDate(new LocalDate("2015-01-01"));
        reservationRequestModel.setSpecificationType(SpecificationType.ADHOC_ROOM);
        reservationRequestModel.setDuration(new Period("PT1H"));
        reservationRequestModel.setPeriodicityType(PeriodicDateTimeSlot.PeriodicityType.MONTHLY);
        ReservationRequestValidator.validatePeriodicSlotsStart(reservationRequestModel, null);
    }
}
