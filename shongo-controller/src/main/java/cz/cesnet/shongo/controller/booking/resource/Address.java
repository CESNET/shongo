package cz.cesnet.shongo.controller.booking.resource;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.api.AbstractComplexType;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * Represents a physical address of a device (IP address or URL).
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Embeddable
public class Address
{
    /**
     * Address value.
     */
    private String value;

    /**
     * Constructor for empty value.
     */
    public Address()
    {
    }

    /**
     * Constructor.
     *
     * @param value sets the {@link #value}
     */
    public Address(String value)
    {
        this.value = value;
    }

    /**
     * @return {@link #value}
     */
    @Column(length = AbstractComplexType.DEFAULT_COLUMN_LENGTH)
    public String getValue()
    {
        return value;
    }

    /**
     * @param value sets the {@link #value}
     */
    public void setValue(String value)
    {
        this.value = value;
    }
}
