package cz.cesnet.shongo.client.web;

import java.util.LinkedList;
import java.util.List;

/**
 * Pages for {@link cz.cesnet.shongo.client.web.controllers.WizardController}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public enum WizardPage
{
    WIZARD_SELECT(null,
            ClientWebUrl.WIZARD_SELECT,
            "views.wizard.page.select", Option.FORCE_ENABLED),

    WIZARD_RESERVATION_REQUEST(
            WIZARD_SELECT,
            ClientWebUrl.WIZARD_RESERVATION_REQUEST_LIST,
            "views.wizard.page.reservationRequestList"),
    WIZARD_RESERVATION_REQUEST_DETAIL(
            WIZARD_RESERVATION_REQUEST,
            null,
            "views.wizard.page.reservationRequestDetail"),

    WIZARD_CREATE_ROOM(
            WIZARD_SELECT,
            ClientWebUrl.WIZARD_CREATE_ROOM,
            "views.wizard.page.createRoom", Option.NEXT_REQUIRED_ENABLED),
    WIZARD_CREATE_ROOM_ATTRIBUTES(
            WIZARD_CREATE_ROOM,
            ClientWebUrl.WIZARD_CREATE_ROOM_ATTRIBUTES,
            "views.wizard.page.createRoom.attributes"),
    WIZARD_CREATE_ROOM_ROLES(
            WIZARD_CREATE_ROOM_ATTRIBUTES,
            ClientWebUrl.WIZARD_CREATE_ROOM_ROLES,
            "views.wizard.page.createRoom.roles"),
    WIZARD_CREATE_ROOM_CONFIRM(
            WIZARD_CREATE_ROOM_ROLES,
            ClientWebUrl.WIZARD_CREATE_CONFIRM,
            "views.wizard.page.createConfirm"),
    WIZARD_CREATE_ROOM_DETAIL(
            WIZARD_CREATE_ROOM_CONFIRM,
            ClientWebUrl.WIZARD_CREATE_DETAIL,
            "views.wizard.page.createDetail", Option.RESET_ENABLED),

    WIZARD_CREATE_PERMANENT_ROOM_CAPACITY(
            WIZARD_SELECT,
            ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY,
            "views.wizard.page.createPermanentRoomCapacity"),
    WIZARD_CREATE_PERMANENT_ROOM_CAPACITY_CONFIRM(
            WIZARD_CREATE_PERMANENT_ROOM_CAPACITY,
            ClientWebUrl.WIZARD_CREATE_CONFIRM,
            "views.wizard.page.createConfirm"),
    WIZARD_CREATE_PERMANENT_ROOM_CAPACITY_DETAIL(
            WIZARD_CREATE_PERMANENT_ROOM_CAPACITY_CONFIRM,
            ClientWebUrl.WIZARD_CREATE_DETAIL,
            "views.wizard.page.createDetail", Option.RESET_ENABLED),

    MULTIPLE_NEXT_PAGES("views.wizard.page.multiple");

    private final int options;

    private final Page page;

    private WizardPage previousPage;

    private List<WizardPage> nextPages = new LinkedList<WizardPage>();

    private boolean nextPageMultiple;

    private WizardPage(String titleCode)
    {
        this.page = new Page(null, titleCode);
        this.options = 0;
    }

    private WizardPage(WizardPage previousPage, String url, String titleCode, int options)
    {
        this.page = new Page(url, titleCode);
        this.previousPage = previousPage;
        if (previousPage != null) {
            previousPage.nextPages.add(this);
        }
        this.options = options;
    }

    private WizardPage(WizardPage parentPage, String url, String titleCode)
    {
        this(parentPage, url, titleCode, 0);
    }

    public String getUrl()
    {
        return page.getUrl();
    }

    public String getTitleCode()
    {
        return page.getTitleCode();
    }

    public WizardPage getPreviousPage()
    {
        return previousPage;
    }

    public WizardPage getNextPage()
    {
        switch (nextPages.size()) {
            case 0:
                return null;
            case 1:
                return nextPages.get(0);
            default:
                return MULTIPLE_NEXT_PAGES;
        }
    }

    public boolean isAlwaysEnabled()
    {
        return (options & Option.FORCE_ENABLED) != 0;
    }

    public boolean isPreviousEnabled()
    {
        return (options & Option.RESET_ENABLED) == 0;
    }

    public boolean isNextRequiredEnabled()
    {
        return (options & Option.NEXT_REQUIRED_ENABLED) != 0;
    }

    public static class Option
    {
        /**
         * {@link WizardPage} is always enabled.
         */
        private static final int FORCE_ENABLED = 0x00000001;

        /**
         * Disables all previous {@link WizardPage}s without {@link #FORCE_ENABLED} option.
         */
        private static final int RESET_ENABLED = 0x00000002;

        /**
         * "Continue" button is shown only when the next {@link WizardPage} is enabled.
         */
        private static final int NEXT_REQUIRED_ENABLED = 0x00000004;
    }
}
