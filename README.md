pdf2txtpos
==========

mvn clean dependency:copy-dependencies
mvn package
# You need fontbox version >= 1.8.6
# wget http://central.maven.org/maven2/org/apache/pdfbox/fontbox/1.8.6/fontbox-1.8.6.jar
java -cp "./target/dependency/*:./target/pdf2txtpos-1.0-SNAPSHOT.jar" com.pauldeschacht.pdf2txtpos.PDF2TxtPos
