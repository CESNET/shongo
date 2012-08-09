package cz.cesnet.shongo;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an enumeration of all technologies.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public enum Technology
{
    /**
     * @see <a href="http://en.wikipedia.org/wiki/H.323">H.323</a>
     */
    H323("H.323"),

    /**
     * @see <a href="http://en.wikipedia.org/wiki/Session_Initiation_Protocol">SIP</a>
     */
    SIP("SIP"),

    /**
     * @see <a href="http://www.adobe.com/products/adobeconnect.html">Adobe Connect</a>
     */
    ADOBE_CONNECT("Adobe Connect");

    /**
     * Technology name that is visible to users.
     */
    private String name;

    /**
     * Constructor.
     *
     * @param name Sets the {@link #name}
     */
    private Technology(String name)
    {
        this.name = name;
    }

    /**
     * @return code of technology
     */
    public String getCode()
    {
        return toString();
    }

    /**
     * @return {@link #name}
     */
    public String getName()
    {
        return name;
    }

    /**
     * @return list of all known technologies.
     */
    public static List<Technology> getValues()
    {
        List<Technology> list = new ArrayList<Technology>();
        list.add(H323);
        list.add(SIP);
        list.add(ADOBE_CONNECT);
        return list;
    }
}
