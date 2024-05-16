# Runtime directory of Shongo

This directory can contain runtime files for Shongo components.

## Installation

To install Shongo use the following command:

    service/shongo-install.sh shongo-controller shongo-connector

To uninstall Shongo use the following command:

    service/shongo/uninstall.sh

## Configuration

Create the following configuration files to configure Shongo components:

* <code>shongo-client-cli.cfg.xml</code> to configure Command-Line Client
* <code>shongo-client-connector.cfg.xml</code> to configure connectors
* <code>shongo-client-connector.auth.xml</code> to configure authentication of connectors
* <code>shongo-client-controller.auth.xml</code> to configure Controller

## Tools

Use the following command to run Command-Line Client:

    bin/shongo-client-cli.sh

Use the following command to check status of Shongo component (useful for Nagios NRPE plugin):

    bin/shongo-check.sh [shongo-connector|shongo-controller]
