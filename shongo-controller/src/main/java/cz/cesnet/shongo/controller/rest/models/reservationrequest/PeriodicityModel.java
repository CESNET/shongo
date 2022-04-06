package cz.cesnet.shongo.controller.rest.models.reservationrequest;

import cz.cesnet.shongo.controller.api.PeriodicDateTimeSlot;
import lombok.Data;
import org.joda.time.LocalDate;

import java.util.List;

/**
 * Represents periodicity of reservation.
 *
 * @author Filip Karnis
 */
@Data
public class PeriodicityModel {

    /**
     * Type of the period
     */
    private PeriodicDateTimeSlot.PeriodicityType type;

    /**
     * Cycle of the period
     */
    private int periodicityCycle;

    /**
     * Days of the period for weekly period
     */
    private PeriodicDateTimeSlot.DayOfWeek[] periodicDaysInWeek;

    /**
     * End of the period
     */
    private LocalDate periodicityEnd;

    /**
     * Dates excluded from period
     */
    private List<LocalDate> excludeDates;

    /**
     * Periodicity parameters for specific day in month (e.g. 2. friday in a month)
     */
    private PeriodicDateTimeSlot.PeriodicityType.MonthPeriodicityType monthPeriodicityType;
    private PeriodicDateTimeSlot.DayOfWeek periodicityDayInMonth;
    private int periodicityDayOrder;
}
