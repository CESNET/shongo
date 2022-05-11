package cz.cesnet.shongo.controller.rest.models.resource;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.joda.time.Period;

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
