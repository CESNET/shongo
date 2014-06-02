# Shongo Connector for Devices

## Usage

To run connector application go to directory
<shongo_repository> and type the
following command:

    shongo-deployment/bin/shongo_connector.sh


## Storage configuration

### Shibboleth

    TODO:

### Force downloading of recordings

Write the following to the <code>.htaccess</code> file:

    <FilesMatch "\.[mM][pP]?">
      ForceType application/octet-stream
      Header set Content-Disposition attachment
    </FilesMatch>

