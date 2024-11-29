
# Shongo Controller

## Build

To build the Shongo Controller using Maven, navigate to the `<shongo_repository>` directory and run:

```bash
  mvn package
```

To start the controller on `localhost` at port `8181`, use:

```bash
  shongo-deployment/service/shongo-controller.sh start
```

To connect to the controller, use the client application:

```bash
  shongo-deployment/bin/shongo-client-cli.sh
```

---

## Requirements

The Shongo Controller requires the following to build and run:

1. Java (11)
2. Maven (3.8.7)
3. PostgreSQL Database (11)

### Installing Maven

Download Maven from [Maven Download Page](https://maven.apache.org/download.cgi)
and follow the installation instructions provided there.

### Installing Java on Ubuntu/Debian

To install Java on Ubuntu/Debian system, execute the following commands:

```bash
    sudo apt-get install openjdk-11-jdk
```

For more details, refer to [this guide](http://www.webupd8.org/2012/06/how-to-install-oracle-java-7-in-debian.html).


### Installing PostgreSQL Database

To install PostgreSQL on Ubuntu/Debian, execute the following command:

```bash
  apt-get install postgresql
```

Alternatively, you can run PostgreSQL as a container:

```bash
  docker run --name shongo-postgres -e POSTGRES_PASSWORD=shongo -p 5432:5432 -d postgres:11-alpine
```

---

## Configuring the PostgreSQL Database

1. Connect to the PostgreSQL Console

   You can connect using the `psql` command as the `postgres` system user.

2. Create a Database for Shongo

   ```postgresql
   CREATE DATABASE shongo;
   ```

3. Create a user for connecting to the database:

   ```postgresql
   CREATE USER shongo WITH PASSWORD 'shongo';
   ```

4. Change the database owner to the created user:

   ```postgresql
   ALTER DATABASE shongo OWNER TO shongo;
   ```

5. Configure Authentication

   Add the following lines to the `pg_hba.conf` configuration file:

   ```text
       host    all             all             127.0.0.1/32            md5
       host    all             all             ::1/128                 md5
   ```

   The file is typically located at:

    - `/etc/postgresql/<version>/main/pg_hba.conf` (Debian-based systems)
    - `/var/lib/pgsql/data/pg_hba.conf` (Fedora-based systems)

6. Restart the PostgreSQL server:

   ```bash
   service postgresql restart
   ```

7. Configure Shongo database connection

   Add the following lines to the `shongo-controller.cfg.xml` configuration file:

   ```xml
   <database>
       <driver>org.postgresql.Driver</driver>
       <url>jdbc:postgresql://127.0.0.1/shongo</url>
       <username>shongo</username>
       <password>shongo</password>
   </database>
   ```

---

## Backup and Restore of PostgreSQL Database

### Backup database:

```bash
  su postgres -c "pg_dump <database> > /tmp/shongo.sql"
```

### Restore database:

```bash
  su postgres -c "psql -c \"DROP DATABASE IF EXISTS <database>;\""
  su postgres -c "psql -c \"CREATE DATABASE <database>;\""
  su postgres -c "psql -d <database> < /tmp/shongo.sql"
```

---

## Nagios NRPE

To monitor Shongo with Nagios, follow these steps:

1. Install nagios NRPE server:

   ```bash
   apt-get install nagios-nrpe-server
   ```

2. Update Configuration

   In the `/etc/nagios/nrpe.cfg` configuration file, add `nagios.cesnet.cz` to `allowed_hosts`.

   Also, add the following lines to the file:

   ```text
    command[check_shongo_controller]=<shongo>/shongo-deployment/bin/shongo-check.sh -c shongo-dev.cesnet.cz shongo-controller
    command[check_shongo_connector]=<shongo>/shongo-deployment/bin/shongo-check.sh -c shongo-dev.cesnet.cz shongo-connector <agent-names>
   ```

---

## Hibernate Notes

- **OneToMany** Association

  If a `OneToMany` association lacks the `mappedBy` attribute and is declared as a `java.util.List`,
  deleting one item may cause all items to be deleted and re-inserted.

  See: [Why Hibernate Does Delete All](http://assarconsulting.blogspot.cz/2009/08/why-hibernate-does-delete-all-then-re.html)

- **Mapping** Associations

    - Use field access for mapping associations
    - The `getter` should return `unmodifiableList` or `unmodifiableSet`
    - Avoid setters; instead, use `addXXX` or `removeXXX` methods
    - See: [Unmodifiable Collections with JPA](http://vard-lokkur.blogspot.cz/2011/04/jpa-and-unmodifiable-collections.html)
      and [Hibernate Performance Tips](http://www.javacodegeeks.com/2012/03/hibernate-performance-tips-dirty.html)

- **Bidirectional** Associations

    - Ensure synchronization of both sides of bidirectional associations.
