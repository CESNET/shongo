package cz.cesnet.shongo.controller;

/**
 * {@link Component} which can be switched on/off.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class SwitchableComponent extends Component
{
    /**
     * Specifies whether thsi {@link SwitchableComponent} is enabled (is on).
     */
    private boolean enabled = true;

    /**
     * @return {@link #enabled}
     */
    public boolean isEnabled()
    {
        return enabled;
    }

    /**
     * @param enabled sets the {@link #enabled}
     */
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }
}
