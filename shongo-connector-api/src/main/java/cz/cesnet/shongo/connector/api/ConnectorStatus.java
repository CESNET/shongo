package cz.cesnet.shongo.connector.api;

/**
 * Status of connector.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ConnectorStatus
{
    /**
     * Connector agent name.
     */
    private String name;

    /**
     * @see State
     */
    private State state;

    /**
     * @return {@link #name}
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name sets the {@link #name}
     */
    public void setName(String name)
    {
        this.name = name;
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

    @Override
    public String toString()
    {
        return String.format("ConnectorStatus (name: %s, state: %s)", name, state);
    }

    /**
     * State of the connector.
     */
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
