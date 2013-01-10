#!/usr/bin/perl
#
# Shongo command line client
#
package main;

# Get directory

    use File::Spec::Functions qw(rel2abs);
    use File::Basename qw(dirname);
    my $path   = rel2abs($0);
    our $directory = dirname($path);

use strict;
use warnings;
use IPC::Open2;
use POSIX qw(strftime);

# Change directory to project root
chdir("$directory/..");

# Get scripts
my $filter = $ARGV[0];
my $script_directory = $directory . '/src/test/perl';
my @scripts = ();
opendir(DIRECTORY, $script_directory) or die $!;
while ( my $file = readdir(DIRECTORY) ) {
    if ( $file =~ /.(pl|sh)$/ && (!defined($filter) || $file =~ /$filter/) ) {
        push(@scripts, $file);
    }
}
if ( scalar(@scripts) == 0 ) {
    print("No tests matches '$filter'.\n");
    exit(0);
}
# Sort scripts
@scripts = sort(@scripts);

# Start controller
my $prefix = ">>";
my $started = 0;
print("$prefix Starting controller...\n");
my $pid = open2(\*CONTROLLER_READER, \*CONTROLLER_WRITER, "bin/controller.sh --config client-cli/src/test/resources/controller-test.cfg.xml");
while ( $started == 0 && defined(my $line = <CONTROLLER_READER>)  ) {
    if ( $line =~ /Controller successfully started/ ) {
        $started = 1;
    }
    else {
        print "$prefix $line";
    }
}
if ( $started == 0 ) {
    print("Controller failed to start...\n");
    exit(0);
}
print("$prefix Controller successfully started.\n\n");

# Run test scripts
foreach my $file (@scripts) {
    if ( $file =~ /.pl$/ ) {
        system('perl -w ' . $script_directory . '/' . $file);
    }
    elsif ( $file =~ /.sh$/ ) {
        system('sh ' . $script_directory . '/' . $file);
    }
    else {
        print("Unknown script type '$file'.\n");
    }
}

# Stop controller
print("\n$prefix Stopping controller...\n");
print CONTROLLER_WRITER "exit\n";
while ( defined(my $line = <CONTROLLER_READER>)  ) {
    if ( $started == 0 ) {
        print "$prefix $line";
    }
    if ( $line =~ /Stopping controller/ ) {
        $started = 0;
    }
}
print("$prefix Controller successfully stopped.\n");
close(CONTROLLER_READER);
close(CONTROLLER_WRITER);

system("rm -R data-test/");
