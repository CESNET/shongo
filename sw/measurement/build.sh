DIR=`dirname $0`

cd $DIR

cd parent
mvn install
cd ..

cd common
mvn install
cd ..

cd jxta
mvn package
cd ..

cd jade
mvn package
cd ..

cd fuse
mvn package
cd ..

cd launcher
mvn package
cd ..