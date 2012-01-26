DIR=`dirname $0`

cd $DIR

cd parent
mvn clean
cd ..

cd common
mvn clean
cd ..

cd jxta
mvn clean
cd ..

cd jade
mvn clean
cd ..

cd launcher
mvn clean
cd ..