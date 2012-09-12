package cz.cesnet.shongo.controller.request.report;

import cz.cesnet.shongo.controller.common.Person;
import cz.cesnet.shongo.controller.report.Report;

import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class PersonWithoutEndpointReport extends Report
{
    private Person person;

    public PersonWithoutEndpointReport()
    {
    }

    public PersonWithoutEndpointReport(Person person)
    {
        this.person = person;
    }

    @Override
    @Transient
    public String getText()
    {
        return String.format("%s (%s) hasn't accepted/rejected invitation or hasn't selected an endpoint yet.\n",
                person.getName(), person.getEmail());
    }
}
