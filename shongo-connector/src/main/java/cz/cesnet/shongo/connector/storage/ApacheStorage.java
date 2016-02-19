package cz.cesnet.shongo.connector.storage;

import cz.cesnet.shongo.api.RecordingFolder;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.api.jade.CommandException;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * {@link AbstractStorage} which is implemented by local folder which is published by Apache.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ApacheStorage extends AbstractStorage
{
    public static final String PERMISSION_FILE_NAME = ".htaccess";

    /**
     * @see cz.cesnet.shongo.connector.storage.LocalStorageHandler
     */
    private LocalStorageHandler localStorageHandler;

    /**
     * Format for single user permission which can be used in {@link #PERMISSION_FILE_NAME}.
     */
    private String permissionFormat;

    /**
     * Constructor.
     *
     * @param url                     sets the {@link #url}
     * @param userInformationProvider sets the {@link #userInformationProvider}
     */
    public ApacheStorage(String url, String permissionFormat, URL downloadableUrlBase, UserInformationProvider userInformationProvider) throws FileNotFoundException
    {
        super(url, downloadableUrlBase, userInformationProvider);

        this.localStorageHandler = new LocalStorageHandler(url);
        this.permissionFormat = permissionFormat.trim();
    }

    @Override
    public String createFolder(Folder folder) throws FileNotFoundException {
        return localStorageHandler.createFolder(folder);
    }

    @Override
    public void deleteFolder(String folderId)
    {
        localStorageHandler.deleteFolder(folderId);
    }

    @Override
    public boolean folderExists(String folderId)
    {
        return localStorageHandler.folderExists(folderId);
    }

    @Override
    public List<Folder> listFolders(String folderId, final String folderName)
    {
        return localStorageHandler.listFolders(folderId, folderName);
    }

    public boolean isFolderPermissionsSet(String folderId) throws FileNotFoundException {
        if (!localStorageHandler.rootFolderExists()) {
            throw new FileNotFoundException("Storage directory '" + localStorageHandler.getUrl()  + "' doesn't exist.");

        }
        File permissionsFile = new File();
        permissionsFile.setFileName(PERMISSION_FILE_NAME);
        permissionsFile.setFolderId(folderId);
        return localStorageHandler.fileExists(permissionsFile);
    }

    @Override
    public void setFolderPermissions(String folderId, Map<String, RecordingFolder.UserPermission> userPermissions)
            throws CommandException
    {
        String folderUrl = localStorageHandler.getUrlFromId(folderId);
        String permissionFileUrl = LocalStorageHandler.getChildPath(folderUrl, PERMISSION_FILE_NAME);

        StringBuilder permissionFileContent = preparePermissionFileContent(userPermissions);

        printPermissionsToFile(permissionFileUrl,permissionFileContent);
    }

    public void setFolderPermissions(String folderId, StringBuilder permissionFileContent)
            throws CommandException
    {
        String folderUrl = localStorageHandler.getUrlFromId(folderId);
        String permissionFileUrl = LocalStorageHandler.getChildPath(folderUrl, PERMISSION_FILE_NAME);

        printPermissionsToFile(permissionFileUrl,permissionFileContent);
    }

    private void printPermissionsToFile(String permissionFileUrl, StringBuilder permissionFileContent)
    {        PrintStream out = null;
        try {
            out = new PrintStream(new FileOutputStream(permissionFileUrl));
            out.print(permissionFileContent);
        }
        catch (FileNotFoundException exception) {
            throw new RuntimeException("File '" + permissionFileUrl + "' cannot be written.", exception);
        }
        finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public StringBuilder preparePermissionFileContent(Map<String, RecordingFolder.UserPermission> userPermissions) throws CommandException
    {
        String permissionFormat;
        Set<String> permissionValues = new TreeSet<String>();
        if (this.permissionFormat.contains("${userPrincipalName}")) {
            permissionFormat = this.permissionFormat.replace("${userPrincipalName}", "${permissionValue}");
            for (String userId : new TreeSet<String>(userPermissions.keySet())) {
                UserInformation userInformation = getUserInformation(userId);
                for (String principalName : new TreeSet<String>(userInformation.getPrincipalNames())) {
                    permissionValues.add(principalName);
                }
            }
        }
        else if (this.permissionFormat.contains("${userId}")) {
            permissionFormat = this.permissionFormat.replace("${userId}", "${permissionValue}");
            for (String userId : new TreeSet<String>(userPermissions.keySet())) {
                permissionValues.add(userId);
            }
        }
        else {
            throw new IllegalArgumentException("Illegal permission format '" + this.permissionFormat + "'.");
        }

        StringBuilder permissionFileContent = new StringBuilder();
        for (String permissionValue : permissionValues) {
            permissionFileContent.append(permissionFormat.replace("${permissionValue}", permissionValue));
            permissionFileContent.append("\n");
        }
        return permissionFileContent;
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
    public String getFileDownloadableUrl(String folderId, String fileName) throws MalformedURLException
    {
        String fileUrl = LocalStorageHandler.getChildPath(folderId, fileName);
        if (!folderExists(fileUrl)) {
            return null;
        }
        return new URL(this.getDownloadableUrlBase(), fileUrl).toString();
    }

    @Override
    public boolean validateFile(File file,  long expectedSize)
    {
        return localStorageHandler.validateFile(file, expectedSize);
    }

    @Override
    public boolean filenameEqualsFileId(File file, String fileId)
    {
        return localStorageHandler.filenameEqualsFileId(file, fileId);
    }
}
