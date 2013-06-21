package cz.cesnet.shongo.api;

/**
 * Represents object that can be serialized to/from {@link String}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface AtomicType
{
    /**
     * De-serialize {@link AtomicType} from {@link String}.
     *
     * @param string to be de-serialized from
     */
    public void fromString(String string);

    /**
     * Serialize {@link AtomicType} to {@link String}.
     *
     * @return serialized {@link String}
     */
    public String toString();
}
