plugin-php
==========
This plugin based on the existing implementation of PHP scripting support on dotCMS versions
pior to 3.3.  This uses the latest Resin (including Quercus) library and Apache's BSF support.
This plugin is intended as support for PHP scripting within content and support for PHP files
as assets.

This OSGi bundle includes a PHP view tool (http://dotcms.com/docs/latest/viewtools#viewtool) 
and a PHP servlet to process PHP files.

Contains
========
* Source Code
* bsf.jar (2.4.0)
* resin.jar (4.0.44)

Building
========
To download and build,clone the repo, cd into the cloned directoy and run
```
git clone https://github.com/dotCMS/plugin-php.git
cd ./plugin-php
```

To install all you need to do is build the JAR. to do this run
```
./gradlew jar
```

This will build a jar in the build/libs directory

1. To install this bundle:

Copy the bundle jar file inside the Felix OSGI container (dotCMS/felix/load).
        OR
Upload the bundle jar file using the dotCMS UI (CMS Admin->Dynamic Plugins->Upload Plugin).

2. To uninstall this bundle:

Remove the bundle jar file from the Felix OSGI container (dotCMS/felix/load).
        OR
Undeploy the bundle using the dotCMS UI (CMS Admin->Dynamic Plugins->Undeploy).

Usage
=====

To use the view tool all you need is to add the "key" and eval method to the code, like this:
```
$php.eval("phpinfo();")
```

The location for the servlet used for script file assets evaluation name will be formed like
this <host>/app/SERVLET_NAME where SERVLET_NAME is "phpservlet" by default but can be changed
within the code.

To use the servlet add the path to script to be processed, like this

Script location:
```
<host>/folder1/folder2/script.php
```
With servlet:
```
<host>/app/SERVLER_NAME/folder1/folder2/script.php
```