package cz.cesnet.shongo.connector.storage;

import cz.cesnet.shongo.api.RecordingFolder;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.api.jade.CommandException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * {@link AbstractStorage} which is implemented by local folder which is published by Apache.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ApacheStorage extends AbstractStorage
{
    /**
     * @see cz.cesnet.shongo.connector.storage.LocalStorageHandler
     */
    private LocalStorageHandler localStorageHandler;

    /**
     * Constructor.
     *
     * @param url                     sets the {@link #url}
     * @param userInformationProvider sets the {@link #userInformationProvider}
     */
    public ApacheStorage(String url, String downloadableUrlBase, UserInformationProvider userInformationProvider)
    {
        super(url, downloadableUrlBase, userInformationProvider);

        localStorageHandler = new LocalStorageHandler(url);
    }

    @Override
    public String createFolder(Folder folder)
    {
        return localStorageHandler.createFolder(folder);
    }

    @Override
    public void deleteFolder(String folderId)
    {
        localStorageHandler.deleteFolder(folderId);
    }

    @Override
    public List<Folder> listFolders(String folderId, final String folderName)
    {
        return localStorageHandler.listFolders(folderId, folderName);
    }

    @Override
    public void setFolderPermissions(String folderId, Map<String, RecordingFolder.UserPermission> userPermissions)
            throws CommandException
    {
        String folderUrl = localStorageHandler.getUrlFromId(folderId);
        String permissionFileUrl = LocalStorageHandler.getChildUrl(folderUrl, ".htaccess");

        StringBuilder permissionFileContent = new StringBuilder();
        permissionFileContent.append("require user");
        for (String userId : new TreeSet<String>(userPermissions.keySet())) {
            UserInformation userInformation = getUserInformation(userId);
            for (String principalName : new TreeSet<String>(userInformation.getPrincipalNames())) {
                permissionFileContent.append(" ");
                permissionFileContent.append(principalName);
            }
        }
        PrintStream out = null;
        try {
            out = new PrintStream(new FileOutputStream(permissionFileUrl));
            out.print(permissionFileContent);
        }
        catch (FileNotFoundException exception) {
            throw new RuntimeException("File '" + permissionFileUrl + "' couldn't be written.", exception);
        }
        finally {
            if (out != null) {
                out.close();
            }
        }
    }

    @Override
    public void createFile(File file, InputStream fileContent, ResumeSupport resumeSupport)
    {
        localStorageHandler.createFile(file, fileContent, resumeSupport);
    }

    @Override
    public void deleteFile(String folderId, String fileName)
    {
        localStorageHandler.deleteFile(folderId, fileName);
    }

    @Override
    public boolean fileExists(File file)
    {
        return localStorageHandler.fileExists(file);
    }

    @Override
    public List<File> listFiles(String folderId, String fileName)
    {
        return localStorageHandler.listFiles(folderId, fileName);
    }

    @Override
    public InputStream getFileContent(String folderId, String fileName)
    {
        return localStorageHandler.getFileContent(folderId, fileName);
    }

    @Override
    public String getFileDownloadableUrl(String folderId, String fileName)
    {
        String fileUrl = LocalStorageHandler.getChildUrl(folderId, fileName);
        String downloadableUrl = this.getDownloadableUrlBase() + "/" + fileUrl;
        return downloadableUrl;
    }
}
