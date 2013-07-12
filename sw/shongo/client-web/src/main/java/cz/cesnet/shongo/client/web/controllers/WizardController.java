package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.client.web.ClientWebUrl;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import java.util.LinkedList;
import java.util.List;

/**
 * Controller for displaying wizard interface.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
public class WizardController
{
    /**
     * Handle wizard view.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD, method = RequestMethod.GET)
    public String handleDefault()
    {
        return "forward:" + ClientWebUrl.WIZARD_SELECT;
    }

    @RequestMapping(value = ClientWebUrl.WIZARD_SELECT, method = RequestMethod.GET)
    public ModelAndView handleSelect()
    {
        return handleWizardView(Page.SELECT);
    }

    @RequestMapping(value = ClientWebUrl.WIZARD_RESERVATIONS, method = RequestMethod.GET)
    public ModelAndView handleReservations()
    {
        return handleWizardView(Page.RESERVATIONS);
    }

    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_ROOM, method = RequestMethod.GET)
    public ModelAndView handleCreateRoom()
    {
        return handleWizardView(Page.CREATE_ROOM);
    }

    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_ADHOC_ROOM, method = RequestMethod.GET)
    public ModelAndView handleCreateAdhocRoom()
    {
        return handleWizardView(Page.CREATE_ADHOC_ROOM);
    }

    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM, method = RequestMethod.GET)
    public ModelAndView handleCreatePermanentRoom()
    {
        return handleWizardView(Page.CREATE_PERMANENT_ROOM);
    }

    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY, method = RequestMethod.GET)
    public ModelAndView handleCreatePermanentRoomCapacity()
    {
        return handleWizardView(Page.CREATE_PERMANENT_ROOM_CAPACITY);
    }

    private ModelAndView handleWizardView(Page currentPage)
    {
        List<Page> pages = new LinkedList<Page>();
        pages.add(currentPage);
        Page page = currentPage.getParentPage();
        while (page != null) {
            pages.add(0, page);
            page = page.getParentPage();
        }
        ModelAndView modelAndView = new ModelAndView("wizard");
        modelAndView.addObject("currentPage", currentPage);
        modelAndView.addObject("pages", pages);
        return modelAndView;
    }

    public static enum Page
    {
        SELECT("Select action", ClientWebUrl.WIZARD_SELECT),

        RESERVATIONS(
                SELECT, "Reservations", ClientWebUrl.WIZARD_RESERVATIONS),
        CREATE_ROOM(
                SELECT, "Create room", ClientWebUrl.WIZARD_CREATE_ROOM),

        CREATE_ADHOC_ROOM(
                CREATE_ROOM, "Adhoc", ClientWebUrl.WIZARD_CREATE_ADHOC_ROOM),

        CREATE_PERMANENT_ROOM(
                CREATE_ROOM, "Permanent", ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM),

        CREATE_PERMANENT_ROOM_CAPACITY(
                SELECT, "Create capacity for room",
                ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY);

        private Page parentPage;

        private String titleCode;

        private String url;

        private Page(String titleCode, String url)
        {
            this.titleCode = titleCode;
            this.url = url;
        }

        private Page(Page parentPage, String titleCode, String url)
        {
            this.parentPage = parentPage;
            this.titleCode = titleCode;
            this.url = url;
        }

        public Page getParentPage()
        {
            return parentPage;
        }

        public String getTitleCode()
        {
            return titleCode;
        }

        public void setTitleCode(String titleCode)
        {
            this.titleCode = titleCode;
        }

        public String getUrl()
        {
            return url;
        }

        public void setUrl(String url)
        {
            this.url = url;
        }
    }
}
