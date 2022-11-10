# date-manager-2

This is a simple GUI Swing-based app which can help you to keep track of important dates. 
It is based on TSV-files, it allows import, export, autostart (Windows only), various date sorting methods, i18n, UI scaling.

Basic use case: run it by double-clicking .jar file, add important dates, export them via Export menu (as backup copy), check "Autostart" item in File menu, minimize window.

### Installation

You will need Maven to compile, package and run it.
```
git clone https://github.com/Podbrushkin/date-manager-2.git
cd date-manager-2
mvn clean package
target\DateManager.jar
```

Or execute `mvn clean compile exec:java` to run it without packaging to jar. Autostart function wouldn't be available.
