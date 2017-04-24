package cz.cesnet.shongo.client.web.models;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

/**
 * @author Marek Perichta <mperichta@cesnet.cz>
 */
public class ResourceValidator {

    private static Logger logger = LoggerFactory.getLogger(ResourceValidator.class);

    public void validate(Object object, Errors errors) {
        ResourceModel resourceModel = (ResourceModel) object;
        ResourceType resourceType = resourceModel.getType();

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "validation.field.required");
    }
}
