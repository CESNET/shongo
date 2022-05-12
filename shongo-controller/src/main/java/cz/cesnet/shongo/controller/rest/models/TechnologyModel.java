package cz.cesnet.shongo.controller.rest.models;

import cz.cesnet.shongo.Technology;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Technology of the alias/room reservation request or executable.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public enum TechnologyModel
{

    PEXIP("views.technologyModel.PEXIP", Technology.H323, Technology.SIP,
            Technology.SKYPE_FOR_BUSINESS, Technology.RTMP,
            Technology.WEBRTC),

    /**
     * {@link Technology#ADOBE_CONNECT}
     */
    ADOBE_CONNECT("views.technologyModel.ADOBE_CONNECT", Technology.ADOBE_CONNECT),

    /**
     * {@link Technology#FREEPBX}
     */
    FREEPBX("views.technologyModel.FREEPBX", Technology.FREEPBX),
    /**
     * {@link Technology#H323} and/or {@link Technology#SIP}
     */
    H323_SIP("views.technologyModel.H323_SIP", Technology.H323, Technology.SIP);


    /**
     * Code of the title which can be displayed to user.
     */
    private final String titleCode;

    /**
     * Set of {@link Technology}s which it represents.
     */
    private final Set<Technology> technologies;

    /**
     * Constructor.
     *
     * @param titleCode    sets the {@link #titleCode}
     * @param technologies sets the {@link #technologies}
     */
    TechnologyModel(String titleCode, Technology... technologies)
    {
        this.titleCode = titleCode;
        Set<Technology> technologySet = new HashSet<>(Arrays.asList(technologies));
        this.technologies = Collections.unmodifiableSet(technologySet);
    }

    /**
     * @param technologies which must the returned {@link TechnologyModel} contain
     * @return {@link TechnologyModel} which contains all given {@code technologies}
     */
    public static TechnologyModel find(Set<Technology> technologies)
    {
        if (technologies.size() == 0) {
            return null;
        }
        if (H323_SIP.technologies.containsAll(technologies)) {
            return H323_SIP;
        }
        else if (ADOBE_CONNECT.technologies.containsAll(technologies)) {
            return ADOBE_CONNECT;
        }
        else if (FREEPBX.technologies.containsAll(technologies)) {
            return FREEPBX;
        }
        else if (PEXIP.technologies.containsAll(technologies)) {
            return PEXIP;
        }
        return null;
    }

    /**
     * @return {@link #titleCode}
     */
    public String getTitleCode()
    {
        return titleCode;
    }

    /**
     * @return {@link #technologies}
     */
    public Set<Technology> getTechnologies()
    {
        return technologies;
    }
}
