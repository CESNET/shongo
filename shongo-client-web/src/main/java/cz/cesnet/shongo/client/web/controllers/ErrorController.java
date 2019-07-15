package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.ErrorHandler;
import cz.cesnet.shongo.client.web.models.CommonModel;
import cz.cesnet.shongo.client.web.models.ErrorModel;
import cz.cesnet.shongo.client.web.models.ReportModel;
import cz.cesnet.shongo.client.web.support.BackUrl;
import cz.cesnet.shongo.client.web.support.interceptors.IgnoreDateTimeZone;
import cz.cesnet.shongo.client.web.support.interceptors.NavigationInterceptor;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.rpc.CommonService;
import net.tanesha.recaptcha.ReCaptcha;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpSessionRequiredException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Error controller.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
@SessionAttributes({"error", "report"})
public class ErrorController
{
    private static Logger logger = LoggerFactory.getLogger(ErrorController.class);

    @Resource
    private CommonService commonService;

    @Resource
    private ErrorHandler errorHandler;

    @Resource
    private ReCaptcha reCaptcha;

    /**
     * Handle report problem.
     */
    @RequestMapping(value = ClientWebUrl.REPORT, method = {RequestMethod.GET})
    public ModelAndView handleReport(HttpServletRequest request)
    {
        String requestUrl = BackUrl.getInstance(request).getUrl();
        ModelAndView modelAndView = new ModelAndView("report");
        modelAndView.addObject("report", new ReportModel(requestUrl, reCaptcha, commonService));
        return modelAndView;
    }

    /**
     * Handle report problem.
     */
    @RequestMapping(value = ClientWebUrl.REPORT, method = {RequestMethod.POST})
    public ModelAndView handleReportSubmit(
            HttpServletRequest request,
            SessionStatus sessionStatus,
            SecurityToken securityToken,
            @ModelAttribute("report") ReportModel reportModel,
            BindingResult bindingResult)
    {
        reportModel.validate(bindingResult, request);
        if (bindingResult.hasErrors()) {
            CommonModel.logValidationErrors(logger, bindingResult, securityToken);
            return new ModelAndView("report");
        }
        else {
            boolean isSent = sendReport(reportModel, request);
            if (isSent) {
                sessionStatus.setComplete();
            }
            ModelAndView modelAndView = new ModelAndView("report");
            modelAndView.addObject("isSent", isSent);
            if (!isSent) {
                modelAndView.addObject("report", reportModel);
            }
            return modelAndView;
        }
    }

    /**
     * Handle error view.
     */
    @RequestMapping(value = "/error")
    public ModelAndView handleError(HttpServletRequest request, HttpServletResponse response)
    {
        String requestUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        String message = (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Throwable throwable = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        return errorHandler.handleError(request, response, requestUri, statusCode, message, throwable);
    }

    /**
     * Handle login error view.
     */
    @RequestMapping("/login-error")
    @IgnoreDateTimeZone
    public ModelAndView handleLoginErrorView(HttpServletRequest request, HttpServletResponse response)
    {
        // Update request url to login (we don't want to reference the login-error page itself anywhere)
        String requestUri = (String) request.getAttribute(NavigationInterceptor.REQUEST_URL_REQUEST_ATTRIBUTE);
        requestUri = requestUri.replace("/login-error", ClientWebUrl.LOGIN);
        request.setAttribute(NavigationInterceptor.REQUEST_URL_REQUEST_ATTRIBUTE, requestUri);
        String message = "Login error";
        Integer statusCode = HttpStatus.SC_INTERNAL_SERVER_ERROR;
        Throwable throwable = (Throwable) request.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        return errorHandler.handleError(request, response, requestUri, statusCode, message, throwable);
    }

    /**
     * Handle error report problem.
     */
    @RequestMapping(value = "/error/submit", method = {RequestMethod.POST})
    public ModelAndView handleErrorReportSubmit(
            HttpServletRequest request,
            SessionStatus sessionStatus,
            SecurityToken securityToken,
            @ModelAttribute("error") ErrorModel errorModel,
            @ModelAttribute("report") ReportModel reportModel,
            BindingResult bindingResult)
    {
        reportModel.validate(bindingResult, request);
        if (bindingResult.hasErrors()) {
            CommonModel.logValidationErrors(logger, bindingResult, securityToken);
            ModelAndView modelAndView = new ModelAndView("error");
            modelAndView.addObject("error", errorModel);
            modelAndView.addObject("report", reportModel);
            return modelAndView;
        }
        else {
            boolean isSent = sendReport(reportModel, request);
            if (isSent) {
                sessionStatus.setComplete();
            }
            ModelAndView modelAndView = new ModelAndView("report");
            modelAndView.addObject("isSent", isSent);
            if (!isSent) {
                modelAndView.addObject("report", reportModel);
            }
            return modelAndView;
        }
    }

    /**
     * Handle error not found.
     */
    @RequestMapping("/error-not-found")
    public String handleErrorNotFound(HttpServletRequest request)
    {
        logger.error(request.getRemoteAddr() + " " + request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI).toString());
        return "errorNotFound";
    }

    /**
     * Raise test error.
     */
    @RequestMapping(value = "/development/error")
    public String handleTestError()
    {
        throw new RuntimeException("Error");
    }

    /**
     * Handle missing session attributes.
     */
    @ExceptionHandler(HttpSessionRequiredException.class)
    public Object handleExceptions(Exception exception)
    {
        logger.warn("Redirecting to " + ClientWebUrl.HOME + ".", exception);
        return "redirect:" + ClientWebUrl.HOME;
    }

    /**
     * Send report.
     *
     * @param reportModel
     * @param request
     */
    private boolean sendReport(ReportModel reportModel, HttpServletRequest request)
    {
        String emailReplyTo = reportModel.getEmail();
        String emailSubject = reportModel.getEmailSubject();
        String emailContent = reportModel.getEmailContent(request);
        return errorHandler.sendEmailToAdministrator(emailReplyTo, emailSubject, emailContent);
    }
}
