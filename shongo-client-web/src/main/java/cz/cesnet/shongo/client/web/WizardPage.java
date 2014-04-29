package cz.cesnet.shongo.client.web;

import cz.cesnet.shongo.client.web.controllers.AbstractWizardController;
import cz.cesnet.shongo.client.web.support.NavigationPage;

/**
 * Represents a {@link NavigationPage} for {@link AbstractWizardController}.
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
     * Constructor.
     *
     * @param id        sets the {@link #id}
     * @param url       sets the {@link #url}
     * @param titleCode sets the {@link #titleCode}
     * @param titleArgument sets the {@link #titleArguments}
     */
    public WizardPage(Object id, String url, String titleCode, String titleArgument)
    {
        super(url, titleCode, new Object[]{titleArgument});
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
     * @return {@link #titleArguments} as string
     */
    public String getTitleArgumentsAsString()
    {
        if (this.titleArguments == null) {
            return "";
        }
        StringBuilder titleArguments = new StringBuilder();
        for (Object titleArgument : this.titleArguments) {
            if (titleArguments.length() > 0) {
                titleArguments.append(",");
            }
            titleArguments.append(titleArgument);
        }
        return titleArguments.toString();
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
