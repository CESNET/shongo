#
# Command for testing this script
#
#   clear \
#     && sudo chown -R shongo:shongo /home/shongo/shongo \
#     && sudo sh /home/shongo/shongo/sw/shongo/bin/setup-client-web.sh \
#     && sudo su www-data -c "/home/shongo/shongo/sw/shongo/client-web/src/public/index.pl"
#

# Go to sw/shongo
cd `dirname $0`/../

# Setup data directory
mkdir -p data/log/
touch data/log/client-web.log
chown -R shongo:shongo data

# Deny all access for group and other
sudo chmod -R go-rwx ../../

# Allow access to client web for "www-data" user
sudo chown :www-data client-common client-web ./ ../ ../../ data data/log
sudo chmod g+rx client-common client-web ./ ../ ../../ data data/log

# Allow access to client-web log and configuration
sudo chown :www-data data/log/client-web.log
sudo chmod g+rw data/log/client-web.log
if [ -f client-web.cfg.xml ];
then
  chown :www-data client-web.cfg.xml
  chmod g+r client-web.cfg.xml	
fi

# Allow access to client web sources for "www-data" user
sudo chown -R :www-data client-web/src client-common/src
sudo chmod -R g+r client-web/src client-common/src
sudo chmod g+x client-web/src/public/index.pl `find client-web/src client-common/src -type d`

