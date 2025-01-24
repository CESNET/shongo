package cz.cesnet.shongo.controller.rest.models.resource;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.joda.time.Period;

/**
 * Represents a unit of time to use as a period for utilization of {@link ResourceCapacity}.
 *
 * @author Filip Karnis
 */
@Getter
@AllArgsConstructor
public enum Unit
{

    DAY(Period.days(1)),
    WEEK(Period.weeks(1)),
    MONTH(Period.months(1)),
    YEAR(Period.years(1));

    private final Period period;
}
