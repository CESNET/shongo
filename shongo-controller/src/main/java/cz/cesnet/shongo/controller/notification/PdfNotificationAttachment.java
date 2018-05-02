package cz.cesnet.shongo.controller.notification;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Marek Perichta <mperichta@cesnet.cz>
 */
public class PdfNotificationAttachment extends NotificationAttachment {

    private final String filePath;

    /**
     * Constructor.
     *
     * @param fileName sets the desired {@link #fileName} in notification
     * @param filePath sets the {@link #filePath} for pdf to be read and sent
     */
    public PdfNotificationAttachment(String fileName, String filePath)
    {
        super(fileName);
        this.filePath = filePath;
    }


    public byte[] getFileContent () throws IOException {

        //Path pdfPath = Paths.get(filePath);
        Path pdfPath = Paths.get(filePath);
        byte[] pdf = Files.readAllBytes(pdfPath);
        return pdf;
    }

}


