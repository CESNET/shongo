package cz.cesnet.shongo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    ADOBE_CONNECT("Adobe Connect"),

    SKYPE("Skype"),

    BIG_BLUE_BUTTON("BigBlueButton"),

    OPEN_MEETINGS("OpenMeetings"),

    WEBEX("Cisco WebEx");

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

    /**
     * @return formatted given {@code #technologies} as string
     */
    public static String formatTechnologies(Set<Technology> technologies)
    {
        StringBuilder builder = new StringBuilder();
        for (Technology technology : technologies) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(technology.getName());
        }
        return builder.toString();
    }

    /**
     * @return formatted given {@code #technologies} as string
     */
    public static String formatTechnologiesVariants(Set<Set<Technology>> technologiesVariants)
    {
        StringBuilder builder = new StringBuilder();
        for (Set<Technology> technologies : technologiesVariants) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append("[");
            builder.append(formatTechnologies(technologies));
            builder.append("]");
        }
        return builder.toString();
    }
}
