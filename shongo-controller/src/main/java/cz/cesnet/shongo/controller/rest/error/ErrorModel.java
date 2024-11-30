package cz.cesnet.shongo.controller.rest.error;

import lombok.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Represents an error for rest error response.
 *
 * @author Filip Karnis
 */
@Value
public class ErrorModel
{

    String error;

    public static ResponseEntity<ErrorModel> createResponseFromException(Exception e, HttpStatus httpStatus)
    {
        ErrorModel errorModel = new ErrorModel(e.getMessage());
        return new ResponseEntity<>(errorModel, httpStatus);
    }
}
