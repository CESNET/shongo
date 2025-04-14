package cz.cesnet.shongo.controller.booking.request.auxdata.tagdata;

import com.fasterxml.jackson.databind.JsonNode;
import cz.cesnet.shongo.controller.api.TagType;
import cz.cesnet.shongo.controller.booking.request.auxdata.AuxDataMerged;
import lombok.extern.slf4j.Slf4j;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class NotifyEmailAuxData extends TagData<List<String>>
{

    public NotifyEmailAuxData(AuxDataMerged auxData)
    {
        super(auxData);
        if (!TagType.NOTIFY_EMAIL.equals(auxData.getType())) {
            throw new IllegalArgumentException("AuxData is not of type NOTIFY_EMAIL");
        }
    }

    @Override
    protected List<String> constructData()
    {
        if (!auxData.getData().isArray()) {
            throw new IllegalArgumentException("Tag data is not an array");
        }
        if (!auxData.getAuxData().isArray()) {
            throw new IllegalArgumentException("AuxData data is not an array");
        }

        List<String> emails = new ArrayList<>();

        for (JsonNode child : auxData.getAuxData()) {
            emails.add(child.asText());
        }
        for (JsonNode child : auxData.getData()) {
            emails.add(child.asText());
        }
        emails.forEach(email -> {
            if (!isValidEmailAddress(email)) {
                throw new IllegalArgumentException("Invalid email address: " + email);
            }
        });

        return emails;
    }

    public static boolean isValidEmailAddress(String email) {
        try {
            InternetAddress emailAddr = new InternetAddress(email);
            emailAddr.validate();
        } catch (AddressException ex) {
            log.info("Invalid email address: " + email, ex);
            return false;
        }
        return true;
    }
}
