package cz.cesnet.shongo.client.web;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.auth.OpenIDConnectAuthenticationToken;
import cz.cesnet.shongo.client.web.auth.UserPermission;
import cz.cesnet.shongo.client.web.models.TimeZoneModel;
import cz.cesnet.shongo.client.web.models.UserSession;
import cz.cesnet.shongo.client.web.models.UserSettingsModel;
import cz.cesnet.shongo.client.web.support.Breadcrumb;
import cz.cesnet.shongo.client.web.support.Page;
import cz.cesnet.shongo.client.web.support.ReflectiveResourceBundleMessageSource;
import cz.cesnet.shongo.client.web.support.interceptors.NavigationInterceptor;
import cz.cesnet.shongo.controller.ObjectPermission;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.request.ResourceListRequest;
import cz.cesnet.shongo.controller.api.rpc.ResourceService;
import cz.cesnet.shongo.util.DateTimeFormatter;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.NullCacheStorage;
import freemarker.cache.SoftCacheStorage;
import freemarker.template.*;
import freemarker.template.Configuration;
import org.apache.commons.configuration.*;
import org.joda.time.DateTimeZone;
import org.springframework.context.MessageSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * Design for shongo-client-web.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Design
{
    public static final String LAYOUT_FILE_NAME = "layout.ftl";
    public static final String MAIN_FILE_NAME = "main.ftl";

    /**
     * @see cz.cesnet.shongo.client.web.Cache
     */
    @Resource
    protected Cache cache;

    /**
     * {@link MessageSource} used for application messages.
     */
    @Resource
    protected MessageSource applicationMessageSource;

    /**
     * @see ResourceService
     */
    @Resource
    protected ResourceService resourceService;

    /**
     * @see ApplicationContext
     */
    private ApplicationContext applicationContext = new ApplicationContext();

    /**
     * @see ClientWebConfiguration#getDesignParameters
     */
    private HierarchicalConfiguration configurationParameters;

    /**
     * Folder where design resources are stored.
     */
    private String resourcesFolder;

    /**
     * {@link MessageSource} used for design messages.
     */
    protected MessageSource layoutMessageSource;

    /**
     * Specifies whether templates should be cached.
     */
    private boolean cacheTemplates = true;

    /**
     * @see freemarker.template.ObjectWrapper
     */
    private ObjectWrapper templateObjectWrapper = new freemarker.template.DefaultObjectWrapper();

    /**
     * Template engine configuration.
     */
    private Configuration templateConfiguration = new Configuration();

    /**
     * Cache of templates.
     */
    private Map<String, Template> templateMap = new HashMap<String, Template>();

    /**
     * Constructor.
     *
     * @param configuration
     */
    public Design(ClientWebConfiguration configuration)
    {
        this.resourcesFolder = configuration.getDesignFolder();
        this.configurationParameters = configuration.getDesignParameters();

        // Create message source
        ReflectiveResourceBundleMessageSource messageSource = new ReflectiveResourceBundleMessageSource();
        messageSource.setBasename(resourcesFolder + "/design");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        this.layoutMessageSource = messageSource;

        // Initialize template engine
        try {
            templateConfiguration.setObjectWrapper(templateObjectWrapper);

            if (resourcesFolder.startsWith("file:")) {
                templateConfiguration.setTemplateLoader(new FileTemplateLoader(new File(resourcesFolder.substring(5))));
            }
            else {
                templateConfiguration.setClassForTemplateLoading(Design.class, resourcesFolder);
            }
        }
        catch (Exception exception) {
            throw new RuntimeException("Failed to initialize template engine.", exception);
        }
    }

    public void setApplicationMessageSource(MessageSource applicationMessageSource)
    {
        this.applicationMessageSource = applicationMessageSource;
    }

    /**
     * @return {@link #resourcesFolder}
     */
    public String getResourcesFolder()
    {
        return resourcesFolder;
    }

    /**
     * @return {@link #cacheTemplates}
     */
    public boolean isCacheTemplates()
    {
        return cacheTemplates;
    }

    /**
     * @param cacheTemplates sets the {@link #cacheTemplates}
     */
    public void setCacheTemplates(boolean cacheTemplates)
    {
        this.cacheTemplates = cacheTemplates;
        if (cacheTemplates) {
            templateConfiguration.setCacheStorage(new SoftCacheStorage());
        }
        else {
            // Debug
            templateConfiguration.setCacheStorage(new NullCacheStorage());
        }
    }

    /**
     * @param templateFileName
     * @return {@link Template} for given {@code templateFileName}
     */
    protected synchronized Template getTemplate(String templateFileName)
    {
        Template template = (cacheTemplates ? templateMap.get(templateFileName) : null);
        if (template == null) {
            try {
                template = templateConfiguration.getTemplate(templateFileName);
                templateMap.put(templateFileName, template);
            }
            catch (Exception exception) {
                throw new RuntimeException("Failed to get template " + templateFileName, exception);
            }
        }
        return template;
    }

    /**
     * @param template
     * @param dataModel
     * @return rendered given {@code template} for given {@code dataModel}
     */
    protected String renderTemplate(Template template, Object dataModel)
    {
        try {
            StringWriter stringWriter = new StringWriter();
            template.process(dataModel, stringWriter);
            return stringWriter.toString();
        }
        catch (TemplateException exception) {
            Throwable cause = exception.getCause();
            if (cause != null) {
                throw new RuntimeException(cause);
            }
            else {
                throw new RuntimeException(exception);
            }
        }
        catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * @return rendered main page content
     */
    public String renderTemplateMain(HttpServletRequest request)
    {
        Template template = getTemplate(MAIN_FILE_NAME);
        return renderTemplate(template, new TemplateContext(request));
    }

    /**
     * @param request
     * @param head
     * @param title
     * @param content
     * @return {@link cz.cesnet.shongo.client.web.Design.LayoutContext}
     */
    public LayoutContext createLayoutContext(HttpServletRequest request, String head, String title, String content)
    {
        LayoutContext layoutContext = new LayoutContext(request);
        layoutContext.head = head;
        layoutContext.title = title;
        layoutContext.content = content;
        return layoutContext;
    }

    public static class ApplicationContext
    {
        private String version;

        public ApplicationContext()
        {
            this.version = ClientWeb.getVersion();
        }

        public String getVersion()
        {
            return version;
        }
    }

    public class TemplateContext
    {
        protected String baseUrl;

        protected String requestUrl;

        protected UserSession userSession;

        protected Locale userSessionLocale;

        public TemplateContext(HttpServletRequest request)
        {
            this.baseUrl = request.getContextPath();
            this.requestUrl = (String) request.getAttribute(NavigationInterceptor.REQUEST_URL_REQUEST_ATTRIBUTE);
            this.userSession = UserSession.getInstance(request);
            this.userSessionLocale = userSession.getLocale();
        }

        public String message(String code)
        {
            return layoutMessageSource.getMessage(code, null, userSessionLocale);
        }

        public String message(String code, Object... args)
        {
            return layoutMessageSource.getMessage(code, args, userSessionLocale);
        }

        public String escapeJavaScript(String text)
        {
            text = text.replace("\"", "\\\"");
            return text;
        }

        public ApplicationContext getApp()
        {
            return applicationContext;
        }

        public class ConfigurationContext implements TemplateHashModel
        {
            private final org.apache.commons.configuration.Configuration configuration;

            public ConfigurationContext(org.apache.commons.configuration.Configuration configuration)
            {
                this.configuration = configuration;
            }

            @Override
            public TemplateModel get(String key) throws TemplateModelException
            {
                String value = this.configuration.getString(key);
                if (value == null) {
                    return null;
                }
                else if (value.equals("true") || value.equals("false")) {
                    return templateObjectWrapper.wrap(Boolean.valueOf(value));
                }
                else {
                    return templateObjectWrapper.wrap(value);
                }
            }

            @Override
            public boolean isEmpty() throws TemplateModelException
            {
                return this.configuration.isEmpty();
            }
        }

        private ConfigurationContext configurationContext;

        public ConfigurationContext getConfiguration()
        {
            if (configurationContext == null) {
                configurationContext = new ConfigurationContext(configurationParameters);
            }
            return configurationContext;
        }

        public class UrlContext
        {
            private String languageUrl;

            public String getHome()
            {
                return baseUrl + ClientWebUrl.HOME;
            }

            public String getChangelog()
            {
                return baseUrl + ClientWebUrl.CHANGELOG;
            }

            public String getHelp()
            {
                return baseUrl + ClientWebUrl.HELP;
            }

            public String getReport()
            {
                return baseUrl + applyBackUrl(ClientWebUrl.REPORT);
            }

            public String getResources()
            {
                return baseUrl + "/design";
            }

            public String getLanguage()
            {
                if (languageUrl == null) {
                    if (requestUrl.contains("?")) {
                        UriComponentsBuilder languageUrlBuilder = UriComponentsBuilder.fromUriString(requestUrl);
                        languageUrlBuilder.replaceQueryParam("lang", ":lang");
                        languageUrl = languageUrlBuilder.build().toUriString();
                    }
                    else {
                        languageUrl = requestUrl + "?lang=:lang";
                    }
                }
                return baseUrl + languageUrl;
            }

            public String getLanguageEn()
            {
                return getLanguage().replace(":lang", "en");
            }

            public String getLanguageCs()
            {
                return getLanguage().replace(":lang", "cs");
            }

            public String getLogin()
            {
                return baseUrl + ClientWebUrl.LOGIN;
            }

            public String getLogout()
            {
                return baseUrl + ClientWebUrl.LOGOUT;
            }

            public String getUserSettings()
            {
                return baseUrl + applyBackUrl(ClientWebUrl.USER_SETTINGS);
            }

            public String userSettingsAdvancedMode(boolean advanceMode)
            {
                return baseUrl + applyBackUrl(ClientWebUrl.format(ClientWebUrl.USER_SETTINGS_ATTRIBUTE, "userInterface",
                        (advanceMode ? UserSettingsModel.UserInterface.ADVANCED
                                 : UserSettingsModel.UserInterface.BEGINNER)));
            }

            public String userSettingsAdministrationMode(boolean administrationMode)
            {
                return baseUrl + applyBackUrl(ClientWebUrl.format(
                        ClientWebUrl.USER_SETTINGS_ATTRIBUTE, "administrationMode", administrationMode));
            }

            private String applyBackUrl(String url)
            {
                try {
                    return url + "?back-url=" + UriUtils.encodeQueryParam(requestUrl, "utf8");
                }
                catch (UnsupportedEncodingException exception) {
                    throw new RuntimeException(exception);
                }
            }
        }

        private UrlContext urlContext;

        public UrlContext getUrl()
        {
            if (urlContext == null) {
                urlContext = new UrlContext();
            }
            return urlContext;
        }

        public class SessionContext
        {
            private TimezoneContext timezoneContext;

            public TimezoneContext getTimezone()
            {
                if (timezoneContext == null) {
                    timezoneContext = new TimezoneContext();
                }
                return timezoneContext;
            }

            public class TimezoneContext
            {
                public String getTitle()
                {
                    DateTimeZone timeZone = userSession.getTimeZone();
                    if (timeZone == null) {
                        return "";
                    }
                    else {
                        return TimeZoneModel.formatTimeZone(timeZone);
                    }
                }

                public String getHelp()
                {
                    DateTimeZone timeZone = userSession.getTimeZone();
                    DateTimeZone homeTimeZone = userSession.getHomeTimeZone();
                    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.getInstance(DateTimeFormatter.Type.SHORT);

                    StringBuilder help = new StringBuilder();
                    help.append("<table>");
                    help.append("<tr><td align='left' colspan='2'><b style='text-align: left;'>");
                    help.append(applicationMessageSource.getMessage("views.layout.timezone", null, userSessionLocale));
                    help.append("</b></td></tr>");
                    // Current timezone
                    help.append("<tr>");
                    help.append("<td style='text-align: right; vertical-align: top;'>");
                    help.append(applicationMessageSource.getMessage("views.layout.timezone.current", null,
                            userSessionLocale));
                    help.append(":</td>");
                    help.append("<td style='text-align: left;'><b>");
                    help.append(TimeZoneModel.formatTimeZone(timeZone));
                    help.append("</b>");
                    String timeZoneName = TimeZoneModel.formatTimeZoneName(timeZone, userSessionLocale);
                    if (timeZoneName != null) {
                        help.append(" (");
                        help.append(timeZoneName);
                        help.append(")");
                    }
                    // Difference between Current and Home
                    if (homeTimeZone != null && !homeTimeZone.equals(timeZone)) {
                        help.append(", ");
                        help.append("</td></tr>");
                        help.append(applicationMessageSource
                                .getMessage("views.layout.timezone.diff", null, userSessionLocale));
                        help.append(":&nbsp;");
                        help.append(dateTimeFormatter.formatDurationTime(userSession.getTimeZoneOffset()));
                    }
                    help.append("</td></tr>");
                    // Home timezone
                    if (homeTimeZone != null && !homeTimeZone.equals(timeZone)) {
                        help.append("<tr>");
                        help.append("<td style='text-align: right; vertical-align: top;'>");
                        help.append(applicationMessageSource
                                .getMessage("views.layout.timezone.home", null, userSessionLocale));
                        help.append("<td align='left'>");
                        help.append("<b>");
                        help.append(TimeZoneModel.formatTimeZone(homeTimeZone));
                        help.append("</b>");
                        String homeTimeZoneName = TimeZoneModel.formatTimeZoneName(timeZone, userSessionLocale);
                        if (homeTimeZoneName != null) {
                            help.append(" (");
                            help.append(homeTimeZoneName);
                            help.append(")");
                        }
                        help.append("</td></tr>");
                    }
                    help.append("</table>");
                    return help.toString();
                }
            }

            private LocaleContext localeContext;

            public LocaleContext getLocale()
            {
                if (localeContext == null) {
                    localeContext = new LocaleContext();
                }
                return localeContext;
            }

            public class LocaleContext
            {
                public String getTitle()
                {
                    return userSession.getLocale().getDisplayLanguage();
                }

                public String getLanguage()
                {
                    return userSession.getLocale().getLanguage();
                }
            }
        }

        private SessionContext sessionContext;

        public SessionContext getSession()
        {
            if (sessionContext == null) {
                sessionContext = new SessionContext();
            }
            return sessionContext;
        }

        public class UserContext
        {
            private SecurityToken securityToken;

            private UserInformation userInformation;

            public UserContext(SecurityToken securityToken, UserInformation userInformation)
            {
                this.securityToken = securityToken;
                this.userInformation = userInformation;
            }

            public boolean isAdvancedMode()
            {
                return userSession.isAdvancedUserInterface();
            }

            public boolean isAdministrationMode()
            {
                return userSession.isAdministrationMode();
            }

            public boolean isAdministrationModeAvailable()
            {
                return cache.hasUserPermission(securityToken, UserPermission.ADMINISTRATION);
            }

            public boolean isReservationAvailable()
            {
                return cache.hasUserPermission(securityToken, UserPermission.RESERVATION);
            }

            public String getId()
            {
                return userInformation.getUserId();
            }

            public String getName()
            {
                return userInformation.getFullName();
            }
        }

        private UserContext userContext;

        public UserContext getUser()
        {
            if (userContext == null && userSession != null) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication instanceof OpenIDConnectAuthenticationToken) {
                    OpenIDConnectAuthenticationToken token = (OpenIDConnectAuthenticationToken) authentication;
                    userContext = new UserContext(token.getSecurityToken(), token.getPrincipal());
                }
            }
            return userContext;
        }

        public boolean isUserAuthenticated()
        {
            return getUser() != null;
        }
    }

    public class LayoutContext extends TemplateContext
    {
        private String head;

        private String title;

        private String content;

        private Breadcrumb breadcrumb;

        private LayoutContext(HttpServletRequest request)
        {
            super(request);
            this.breadcrumb = (Breadcrumb) request.getAttribute("breadcrumb");
        }

        public String getHead()
        {
            return head;
        }

        public String getTitle()
        {
            return title;
        }

        public String getContent()
        {
            return content;
        }

        public Collection<LinkContext> getLinks()
        {
            List<LinkContext> links = new LinkedList<LinkContext>();
            if (isUserAuthenticated()) {
                links.add(new LinkContext("navigation.userSettings", getUrl().applyBackUrl(ClientWebUrl.USER_SETTINGS)));
            }
            links.add(new LinkContext("navigation.help", getUrl().applyBackUrl(ClientWebUrl.HELP)));
            links.add(new LinkContext("navigation.doc", getUrl().applyBackUrl(ClientWebUrl.DOCUMENTATION)));
            if (isUserAuthenticated()) {
                final UserContext user = getUser();

                if (cache.hasUserPermission(user.securityToken, UserPermission.RESOURCE_MANAGEMENT)) {
                    links.add(new LinkSeparatorContext());
                    links.add(new LinkContext("navigation.resourceManagement", new LinkedList<LinkContext>(){{
                        add(new LinkContext("navigation.resourceCapacityUtilization",
                                ClientWebUrl.RESOURCE_CAPACITY_UTILIZATION));
                        add(new LinkSeparatorContext());
                        add(new LinkContext("navigation.resourceReservations",
                                ClientWebUrl.RESOURCE_RESERVATIONS_VIEW));
                        // Show reservation request link only when there are some valid resources
                        ResourceListRequest listRequest = new ResourceListRequest(user.securityToken);
                        listRequest.setPermission(ObjectPermission.CONTROL_RESOURCE);
                        listRequest.setNeedsConfirmation(true);
                        listRequest.setOnlyLocal(true);
                        if (!resourceService.listResources(listRequest).getItems().isEmpty()) {
                            add(new LinkSeparatorContext());
                            add(new LinkContext("navigation.resourceReservationsConfirmation",
                                    ClientWebUrl.RESERVATION_REQUEST_CONFIRMATION));
                        }
                    }}));
                }
            }
            return links;
        }

        public Iterator<BreadcrumbContext> getBreadcrumbs()
        {
            if (breadcrumb != null) {
                final BreadcrumbContext breadcrumbContext = new BreadcrumbContext();
                return new Iterator<BreadcrumbContext>()
                {
                    private Iterator<Page> iterator = breadcrumb.iterator();

                    @Override
                    public boolean hasNext()
                    {
                        return iterator.hasNext();
                    }

                    @Override
                    public BreadcrumbContext next()
                    {
                        breadcrumbContext.page = iterator.next();
                        return breadcrumbContext;
                    }

                    @Override
                    public void remove()
                    {
                        iterator.remove();
                    }

                };
            }
            else {
                // Empty iterator
                return new Iterator<BreadcrumbContext>() {
                    public boolean hasNext() { return false; }
                    public BreadcrumbContext next() { throw new NoSuchElementException(); }
                    public void remove() { throw new IllegalStateException(); }
                };
            }
        }

        public class LinkContext
        {
            private String titleCode;

            private String url;

            private List<LinkContext> childLinks;

            public LinkContext(String titleCode, String url)
            {
                this.titleCode = titleCode;
                this.url = url;
            }

            public LinkContext(String titleCode, List<LinkContext> childLinks)
            {
                this.titleCode = titleCode;
                this.childLinks = childLinks;
            }

            public String getTitle()
            {
                return applicationMessageSource.getMessage(titleCode, null, userSessionLocale);
            }

            public String getUrl()
            {
                return baseUrl + url;
            }

            public boolean hasChildLinks()
            {
                return childLinks != null && !childLinks.isEmpty();
            }

            public List<LinkContext> getChildLinks()
            {
                if (childLinks == null) {
                    return Collections.emptyList();
                }
                else {
                    return childLinks;
                }
            }

            public boolean isSeparator()
            {
                return false;
            }
        }

        public class LinkSeparatorContext extends LinkContext
        {
            public LinkSeparatorContext()
            {
                super(null, (String) null);
            }

            @Override
            public boolean isSeparator()
            {
                return true;
            }
        }

        public class BreadcrumbContext
        {
            private Page page;

            public String getUrl()
            {
                String pageUrl = page.getUrl();
                if (pageUrl == null) {
                    return null;
                }
                return baseUrl + pageUrl;
            }

            public String getTitle()
            {
                return applicationMessageSource.getMessage(
                        page.getTitleCode(), page.getTitleArguments(), userSessionLocale);
            }
        }
    }
}
