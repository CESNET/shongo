
# Maven Shongo Repository

## Installing the Library

A new library can be installed using the following command:

```bash
  mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file \
          -DlocalRepositoryPath=/home/martin/project/cesnet/shongo/shongo-common/lib/ \
          -Dfile=/home/martin/Downloads/jade.jar \
          -DgroupId=com.tilab.jade \
          -DartifactId=jade \
          -Dversion=4.1.1 \
          -Dpackaging=jar \
          -DcreateChecksum=true
```

## Installing Library Sources

Library sources can be installed with this command:

```bash
  mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file \
          -DlocalRepositoryPath=/home/martin/project/cesnet/shongo/shongo-common/lib/ \
          -Dfile=/home/martin/Downloads/jade-src.jar \
          -DgroupId=com.tilab.jade \
          -DartifactId=jade \
          -Dversion=4.1.1 \
          -Dclassifier=sources \
          -Dpackaging=jar \
          -DcreateChecksum=true
```

## Installing a Custom Maven Plugin

To install a custom Maven plugin, use the following command:

```bash
  mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file \
          -DlocalRepositoryPath=/home/martin/project/cesnet/shongo/shongo-common/lib/ \
          -Dfile=tool-maven-plugin-exec/target/tool-maven-plugin-exec-1.0.0.jar \
          -DpomFile=tool-maven-plugin-exec/pom.xml \
          -Dpackaging=maven-plugin \
          -DcreateChecksum=true
```
