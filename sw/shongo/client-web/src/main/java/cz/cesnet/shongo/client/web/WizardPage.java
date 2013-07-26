package cz.cesnet.shongo.client.web;

/**
 * Represents a {@link NavigationPage} for {@link cz.cesnet.shongo.client.web.controllers.AbstractWizardController}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class WizardPage extends NavigationPage
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
     * Description of page title.
     */
    private String titleDescription;

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

    /**
     * @return {@link #titleDescription}
     */
    public String getTitleDescription()
    {
        return titleDescription;
    }

    /**
     * @param titleDescription sets the {@link #titleDescription}
     */
    public void setTitleDescription(String titleDescription)
    {
        this.titleDescription = titleDescription;
    }
}
