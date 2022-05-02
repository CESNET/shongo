package cz.cesnet.shongo.controller.rest.error;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.ControllerReportSet.ReservationRequestNotDeletableException;
import cz.cesnet.shongo.controller.ControllerReportSet.ReservationRequestDeletedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.mail.MessagingException;

/**
 * Handler for exceptions thrown while processing Rest request.
 *
 * @author Filip Karnis
 */
@RestControllerAdvice
public class ControllerExceptionHandler
{

    @ExceptionHandler(TodoImplementException.class)
    public ResponseEntity<ErrorModel> handleTodo(TodoImplementException e) {
        return ErrorModel.createResponseFromException(e, HttpStatus.NOT_IMPLEMENTED);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ErrorModel> handleUnsupportedOperation(UnsupportedOperationException e) {
        return ErrorModel.createResponseFromException(e, HttpStatus.NOT_IMPLEMENTED);
    }

    @ExceptionHandler(MessagingException.class)
    public ResponseEntity<ErrorModel> handleMassaging(MessagingException e) {
        return ErrorModel.createResponseFromException(e, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(LastOwnerRoleNotDeletableException.class)
    public ResponseEntity<ErrorModel> handleLastOwnerRoleNotDeletable(LastOwnerRoleNotDeletableException e) {
        return ErrorModel.createResponseFromException(e, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ReservationRequestNotDeletableException.class)
    public ResponseEntity<ErrorModel> handleReservationRequestNotDeletable(ReservationRequestNotDeletableException e) {
        return ErrorModel.createResponseFromException(e, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ObjectInaccessibleException.class)
    public ResponseEntity<ErrorModel> handleObjectInaccessible(ObjectInaccessibleException e) {
        return ErrorModel.createResponseFromException(e, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ReservationRequestDeletedException.class)
    public ResponseEntity<ErrorModel> handleReservationRequestDeleted(ReservationRequestDeletedException e) {
        return ErrorModel.createResponseFromException(e, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(CommonReportSet.ObjectNotExistsException.class)
    public ResponseEntity<ErrorModel> handleObjectNotExists(CommonReportSet.ObjectNotExistsException e) {
        return ErrorModel.createResponseFromException(e, HttpStatus.NOT_FOUND);
    }
}
