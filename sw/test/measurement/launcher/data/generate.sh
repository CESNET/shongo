DIR=`dirname $0`

rm -R $DIR/../src/main/java/cz/cesnet/shongo/measurement/launcher/xml
java -jar $DIR/../lib/trang.jar -I xml -O xsd $DIR/schema.xml $DIR/schema.xsd
xjc -d $DIR/../src/main/java -p cz.cesnet.shongo.measurement.launcher.xml $DIR/schema.xsd