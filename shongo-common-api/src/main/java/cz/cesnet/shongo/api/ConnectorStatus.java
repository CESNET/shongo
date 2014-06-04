package cz.cesnet.shongo.api;

import jade.content.Concept;

/**
 * Status of connector.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ConnectorStatus extends AbstractComplexType implements Concept
{
    /**
     * @see State
     */
    private State state;

    /**
     * Constructor.
     */
    public ConnectorStatus()
    {
    }

    /**
     * Constructor.
     *
     * @param state sets the {@link #state}
     */
    public ConnectorStatus(State state)
    {
        this.state = state;
    }

    /**
     * @return {@link #state}
     */
    public State getState()
    {
        return state;
    }

    /**
     * @param state sets the {@link #state}
     */
    public void setState(State state)
    {
        this.state = state;
    }

    private static final String STATE = "state";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(STATE, state);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        state = dataMap.getEnum(STATE, State.class);
    }

    @Override
    public String toString()
    {
        return String.format("ConnectorStatus (state: %s)", state);
    }

    /**
     * State of the connector.
     */
    @jade.content.onto.annotations.Element(name = "ConnectorStatusState")
    public static enum State
    {
        /**
         * Connector can be used (e.g., is connected to controlled device).
         */
        AVAILABLE,

        /**
         * Connector can't be used (e.g., is not connected controlled device).
         */
        NOT_AVAILABLE
    }
}
