del *.class &&^
javac *.java &&^
jar --main-class=NapominalkaTable --create --file=napom.jar *.class *.properties birthdayCakeIcon.png &&^
java -jar napom.jar
cmd