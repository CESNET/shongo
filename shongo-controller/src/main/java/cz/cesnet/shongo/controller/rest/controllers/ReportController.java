package cz.cesnet.shongo.controller.rest.controllers;

import cz.cesnet.shongo.controller.rest.ClientWebUrl;
import cz.cesnet.shongo.controller.rest.ErrorHandler;
import cz.cesnet.shongo.controller.rest.models.report.ReportModel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.mail.MessagingException;

/**
 * Rest controller for report endpoints.
 *
 * @author Filip Karnis
 */
@Slf4j
@SecurityRequirements
@RestController
@RequestMapping(ClientWebUrl.REPORT)
public class ReportController {

    private final ErrorHandler errorHandler;

    public ReportController(@Autowired ErrorHandler errorHandler)
    {
        this.errorHandler = errorHandler;
    }

    /**
     * Handle problem report.
     */
    @Operation(summary = "Report a problem to administrators.")
    @PostMapping
    public void reportProblem(
            @RequestBody ReportModel reportModel) throws MessagingException
    {
        String emailReplyTo = reportModel.getEmail();
        String emailSubject = reportModel.getEmailSubject();
        String emailContent = reportModel.getEmailContent();

        log.info("Sending problem report: {}", reportModel);
        errorHandler.sendEmailToAdministrator(emailReplyTo, emailSubject, emailContent);
    }
}
