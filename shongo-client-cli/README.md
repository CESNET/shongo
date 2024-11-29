
# Shongo Controller Command-Line Client

## Usage

To run the client application, navigate to the `<shongo_repository>` directory and execute the following command:

```bash
  shongo-deployment/bin/shongo-client-cli.sh
```

For help with the application, use:

```bash
  shongo-deployment/bin/shongo-client-cli.sh --help
```

---

## Requirements

This client requires **Perl** to run, along with the following Perl modules:

1. `Term::ReadKey`
2. `File::Which`
3. `Term::ReadLine::Gnu`
4. `RPC::XML`
5. `XML::Twig`
6. `Text::Table`
7. `DateTime::Format::ISO8601`
8. `JSON`
9. `IO::Socket::SSL` (version 1.56)
10. `LWP::Protocol::https`
11. `Sys::Hostname::FQDN`

### Installing Requirements on Ubuntu/Debian

1. Perl is installed by default on Ubuntu/Debian systems.
   The required modules can be installed using the following command:

    ```bash
    sudo apt-get install libterm-readkey-perl libfile-which-perl \
      libterm-readline-gnu-perl librpc-xml-perl libxml-twig-perl \
      libtext-table-perl libdatetime-format-iso8601-perl \
      libjson-perl liblwp-protocol-https-perl
    ```

2. Additionally, install `RPC::XML` (version 1.70+) via CPAN:

    ```bash
    cpan -i RPC::XML
    ```

### Installing Requirements on macOS (Yosemite)

1. Install Xcode, as Perl is part of the Command Line Tools
2. Install CPAN in Terminal to add modules:

   ```bash
    cpan App::cpanminus
    ```

3. Install all the Perl modules listed above, except number 3 - `Term::ReadLine::Gnu`.
   Instead, install `Term::ReadLine::Perl`:

    ```bash
    sudo cpan -i Term::ReadKey File::Which Term::ReadLine::Perl \
      RPC::XML XML::Twig Text::Table DateTime::Format::ISO8601 \
      JSON IO::Socket::SSL LWP::Protocol::https
    ```

4. If you encounter issues with `Term::ReadLine::Gnu`, refer to [this guide](https://coderwall.com/p/kk0hqw/perl-install-term-readline-gnu-on-osx)

### Installing Requirements on Other Platforms

1. On other platforms, you can use the CPAN utility to install the required modules.
   For example:

    ```bash
    cpan -i Term::ReadKey
    ```

2. Even on Ubuntu/Debian, CPAN can be used to upgrade Perl modules.
   For example, to install the latest version of `IO::Socket::SSL` (requires the `libssl-dev` package):

    ```bash
    sudo apt-get install build-essential
    sudo cpan -i IO::Socket::SSL
    ```
