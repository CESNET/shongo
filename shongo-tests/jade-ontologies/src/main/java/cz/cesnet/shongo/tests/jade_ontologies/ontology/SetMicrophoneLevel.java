package cz.cesnet.shongo.tests.jade_ontologies.ontology;

import jade.content.AgentAction;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class SetMicrophoneLevel implements AgentAction
{
    private int level;

    public SetMicrophoneLevel()
    {
        level = 0;
    }

    public SetMicrophoneLevel(int level)
    {
        this.level = level;
    }

    /**
     * @return current microphone level
     */
    public int getLevel()
    {
        return level;
    }

    /**
     * Sets microphone level of the device.
     * @param level    new microphone level, 0-100
     */
    public void setLevel(int level)
    {
        this.level = level;
    }
}
