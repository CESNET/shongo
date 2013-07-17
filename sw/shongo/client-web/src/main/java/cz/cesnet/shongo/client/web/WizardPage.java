package cz.cesnet.shongo.client.web;

/**
 * Represents a {@link Page} for {@link cz.cesnet.shongo.client.web.controllers.AbstractWizardController}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class WizardPage extends Page
{
    /**
     * Identifier of the page.
     */
    private Object id;

    /**
     * Specifies whether this {@link WizardPage} is available to user.
     */
    private boolean available = false;

    /**
     * Constructor.
     *
     * @param id        sets the {@link #id}
     * @param url       sets the {@link #url}
     * @param titleCode sets the {@link #titleCode}
     */
    public WizardPage(Object id, String url, String titleCode)
    {
        super(url, titleCode);
        this.id = id;
    }

    /**
     * @return {@link #id}
     */
    public Object getId()
    {
        return id;
    }

    /**
     * @return {@link #available}
     */
    public boolean isAvailable()
    {
        return available;
    }

    /**
     * @param available sets the {@link #available}
     */
    public void setAvailable(boolean available)
    {
        this.available = available;
    }
}
