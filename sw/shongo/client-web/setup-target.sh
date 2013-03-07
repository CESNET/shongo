# Go to this folder
cd `dirname $0`

# Create log and setup permissions for it
mkdir -p ../log
chmod go+w ../log

# setup permissions for entry point
chmod a+x target/www/index.pl
