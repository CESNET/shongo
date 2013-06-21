package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.controller.CallInitiation;

import java.util.LinkedList;
import java.util.List;

/**
 * Represents a requested compartment in reservation request.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CompartmentSpecification extends Specification
{
    /**
     * Collection of {@link Specification}s for the {@link CompartmentSpecification}.
     */
    private List<ParticipantSpecification> participantSpecifications = new LinkedList<ParticipantSpecification>();

    /**
     * {@link CallInitiation}
     */
    private CallInitiation callInitiation;

    /**
     * @return {@link #participantSpecifications}
     */
    public List<ParticipantSpecification> getSpecifications()
    {
        return participantSpecifications;
    }

    /**
     * @param participantSpecifications {@link #participantSpecifications}
     */
    public void setSpecifications(List<ParticipantSpecification> participantSpecifications)
    {
        participantSpecifications = participantSpecifications;
    }

    /**
     * @param participantSpecification to be added to the {@link #participantSpecifications}
     */
    public void addSpecification(ParticipantSpecification participantSpecification)
    {
        participantSpecifications.add(participantSpecification);
    }

    /**
     * @return {@link #callInitiation}
     */
    public CallInitiation getCallInitiation()
    {
        return callInitiation;
    }

    /**
     * @param callInitiation sets the {@link #callInitiation}
     */
    public void setCallInitiation(CallInitiation callInitiation)
    {
        this.callInitiation = callInitiation;
    }

    public static final String PARTICIPANT_SPECIFICATIONS = "participantSpecifications";
    public static final String CALL_INITIATION = "callInitiation";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(PARTICIPANT_SPECIFICATIONS, participantSpecifications);
        dataMap.set(CALL_INITIATION, callInitiation);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        participantSpecifications = dataMap.getListRequired(PARTICIPANT_SPECIFICATIONS, ParticipantSpecification.class);
        callInitiation = dataMap.getEnum(CALL_INITIATION, CallInitiation.class);
    }
}
