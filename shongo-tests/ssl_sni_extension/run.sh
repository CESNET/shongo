echo

DIR=/usr/lib/jvm/j2sdk1.6-oracle/bin/
echo Java 1.6 Oracle
$DIR/javac SslSniExtension.java
$DIR/java -cp . SslSniExtension
$DIR/java -cp . SslSniExtension  sni_extension_off

echo

DIR=/usr/lib/jvm/jdk1.7.0_07/bin/
echo Java 1.7 Oracle
$DIR/javac SslSniExtension.java
$DIR/java -cp . SslSniExtension
$DIR/java -cp . SslSniExtension  sni_extension_off

echo