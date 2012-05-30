package cz.cesnet.shongo.controller.resource;

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
    H323("h323", "H.323"),

    /**
     * @see <a href="http://en.wikipedia.org/wiki/Session_Initiation_Protocol">SIP</a>
     */
    SIP("sip", "SIP"),

    /**
     * @see <a href="http://www.adobe.com/products/adobeconnect.html">Adobe Connect</a>
     */
    ADOBE_CONNECT("connect", "Adobe Connect");

    /**
     * Technology unique code.
     */
    private String code;

    /**
     * Technology name that is visible to users.
     */
    private String name;

    /**
     * Constructor.
     *
     * @param code Sets the {@link #code}
     * @param name Sets the {@link #name}
     */
    private Technology(String code, String name)
    {
        this.code = code;
        this.name = name;
    }

    /**
     * @return {@link #code}
     */
    public String getCode()
    {
        return code;
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
