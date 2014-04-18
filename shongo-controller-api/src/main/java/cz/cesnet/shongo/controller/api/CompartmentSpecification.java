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
     * Collection of {@link AbstractParticipant}s for the {@link CompartmentSpecification}.
     */
    private List<AbstractParticipant> participants = new LinkedList<AbstractParticipant>();

    /**
     * {@link CallInitiation}
     */
    private CallInitiation callInitiation;

    /**
     * @return {@link #participants}
     */
    public List<AbstractParticipant> getParticipants()
    {
        return participants;
    }

    /**
     * @param participants sets the {@link #participants}
     */
    public void setParticipants(List<AbstractParticipant> participants)
    {
        this.participants = participants;
    }

    /**
     * @param participant to be added to the {@link #participants}
     */
    public void addParticipant(AbstractParticipant participant)
    {
        participants.add(participant);
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

    public static final String PARTICIPANTS = "participants";
    public static final String CALL_INITIATION = "callInitiation";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(PARTICIPANTS, participants);
        dataMap.set(CALL_INITIATION, callInitiation);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        participants = dataMap.getListRequired(PARTICIPANTS, AbstractParticipant.class);
        callInitiation = dataMap.getEnum(CALL_INITIATION, CallInitiation.class);
    }
}
