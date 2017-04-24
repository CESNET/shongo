package cz.cesnet.shongo.client.web.models;

import com.google.common.base.Strings;
import cz.cesnet.shongo.Technology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

import java.util.List;

/**
 * @author Marek Perichta <mperichta@cesnet.cz>
 */
public class ResourceValidator extends GeneralValidator{

    private static Logger logger = LoggerFactory.getLogger(ResourceValidator.class);

    @Override
    public boolean supports(Class<?> type)
    {
        return ResourceValidator.class.equals(type);
    }

    @Override
    public void validate(Object object, Errors errors) {
        ResourceModel resourceModel = (ResourceModel) object;
        ResourceType resourceType = resourceModel.getType();

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "validation.field.required");
        if (!Strings.isNullOrEmpty(resourceModel.getMaximumFuture())) {
            validatePositiveNum("maximumFuture", errors);
        }

        //Technologies must be set for DeviceResource
        if (resourceModel.getType() == ResourceType.DEVICE_RESOURCE) {
            List<String> technologies = resourceModel.getTechnologies();
            if (technologies == null || technologies.size() < 1) {
                errors.rejectValue("technologies", "validation.field.noTechnologiesSelected");
            }
        }
    }
}
