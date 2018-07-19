package cz.cesnet.shongo;

import java.util.*;

/**
 * Represents an enumeration of all technologies.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public enum Technology
{
    /**
     * Represents all technologies.
     */
    ALL("All"),

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

    /**
     * @see <a href="https://www.freepbx.org/">FreePBX</>
     */
    FREEPBX("Teleconference"),

    SKYPE_FOR_BUSINESS("SkypeForBusiness"),

    RTMP("RTMP"),

    WEBRTC("WebRTC"),

    SKYPE("Skype"),

    BIG_BLUE_BUTTON("BigBlueButton"),

    OPEN_MEETINGS("OpenMeetings"),

    WEBEX("Cisco WebEx"),

    NONE("None");

    /**
     * Technology name that is visible to users.
     */
    private String name;

    /**
     * Constructor.
     *
     * @param name sets the {@link #name}
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
     * @param technology
     * @return true whether this {@link Technology} is compatible with given {@code technology},
     *         false otherwise
     */
    public boolean isCompatibleWith(Technology technology)
    {
        if (equals(technology)) {
            return true;
        }
        if (equals(ALL) || technology.equals(ALL)) {
            return true;
        }
        return false;
    }

    /**
     * @param technologies
     * @return true whether this {@link Technology} is compatible with given {@code technologies},
     *         false otherwise
     */
    public boolean isCompatibleWith(Set<Technology> technologies)
    {
        if (technologies.size() == 0) {
            return true;
        }
        if (equals(ALL)) {
            return true;
        }
        if (technologies.contains(this)) {
            return true;
        }
        if (technologies.contains(ALL)) {
            return true;
        }
        return false;
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
     * @return formatted given {@code technologies} as string
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
     * Recursive implementation of {@link #interconnect(java.util.List)}. Each recursion level process single
     * {@link Set<Technology>} from {@code inputTechnologySets} (at {@code currentIndex}).
     *
     * @param inputTechnologySets  list of technology sets to be interconnected
     * @param outputTechnologySets list of variants of technologies already computed
     * @param currentTechnologySet current (incomplete) variant
     * @param currentIndex         specifies recursive level
     */
    private static void interconnect(List<Set<Technology>> inputTechnologySets,
            Set<Set<Technology>> outputTechnologySets,
            Set<Technology> currentTechnologySet, int currentIndex)
    {
        // Stop recursion
        if (currentIndex < 0) {
            // Finally remove all technologies which are not needed
            for (Iterator<Technology> iterator = currentTechnologySet.iterator(); iterator.hasNext(); ) {
                Technology possibleTechnology = iterator.next();
                // Technology is not needed when each group is connected also by another technology
                for (Set<Technology> inputTechnologySet : inputTechnologySets) {
                    boolean connectedAlsoByAnotherTechnology = false;
                    for (Technology technology : inputTechnologySet) {
                        if (technology.equals(possibleTechnology)) {
                            continue;
                        }
                        if (currentTechnologySet.contains(technology)) {
                            connectedAlsoByAnotherTechnology = true;
                            break;
                        }
                    }
                    // Group is connected only by this technology and thus it cannot be removed
                    if (!connectedAlsoByAnotherTechnology) {
                        possibleTechnology = null;
                        break;
                    }
                }
                // All groups are connected also  by another technology so we can remove possible technology
                if (possibleTechnology != null) {
                    iterator.remove();
                }
            }
            outputTechnologySets.add(currentTechnologySet);
            return;
        }

        // Get current group in recursion
        Set<Technology> inputTechnologySet = inputTechnologySets.get(currentIndex);
        // Build all variants of technology set for current group and call next recursive level
        for (Technology technology : inputTechnologySet) {
            // Build new instance of technologies
            Set<Technology> newTechnologies = new HashSet<Technology>();
            newTechnologies.addAll(currentTechnologySet);
            // Add new technology
            newTechnologies.add(technology);
            // Call next recursive level
            interconnect(inputTechnologySets, outputTechnologySets, newTechnologies, currentIndex - 1);
        }
    }

    /**
     * @param technologySets list of technology sets to be interconnected
     * @return collection of all variants of technologies which interconnects all given technology sets
     */
    public static Collection<Set<Technology>> interconnect(List<Set<Technology>> technologySets)
    {
        Set<Set<Technology>> outputTechnologySets = new HashSet<Set<Technology>>();
        interconnect(technologySets, outputTechnologySets, new HashSet<Technology>(), technologySets.size() - 1);
        return outputTechnologySets;
    }
}
