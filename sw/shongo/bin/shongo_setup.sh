#
# Command for testing this script
#
#   clear \
#     && sudo chown -R shongo:shongo /home/shongo/shongo \
#     && sudo sh /home/shongo/shongo/sw/shongo/bin/shongo_setup.sh \
#     && sudo su www-data -c "/home/shongo/shongo/sw/shongo/client-web/target/www/index.pl"
#

# Go to this folder
cd `dirname $0`/..

# Setup data directory
chown -R shongo:shongo data

# Deny all access for group and other
sudo chmod -R go-rwx ../../

# Allow access to client web for "www-data" user
sudo chown :www-data client-common client-web ./ ../ ../../ log
sudo chmod g+rx client-common client-web ./ ../ ../../ log

# Allow access to client-web log and configuration
sudo g+w log
if [ -f client-web.cfg.xml ];
then
  chown :www-data client-web.cfg.xml
  chmod g+r client-web.cfg.xml	
fi

# Allow access to client web sources for "www-data" user
sudo chown -R :www-data client-web/target
sudo chmod -R g+r client-web/target
sudo chmod g+x client-web/target/www/index.pl `find client-web/target -type d`
