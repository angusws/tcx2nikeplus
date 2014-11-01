Development/Build Prerequisites
===============================
* *nix system
* java sdk (1.7+)
* maven 3


Directory Setup
===============
You may require the following property files, depending on what you want to do:
* download garmin activities.
```
$ cat etc/garmin.properties
# Credentials for garmin connect login.
GARMIN_USERNAME = <garmin-connect-username>
GARMIN_PASSWORD = <garmin-connect-password>
```
* upload to nike+.
```
$ cat etc/nikeplus.properties
# Credentials for nike+ login
# I discovered these values by sniffing HTTP posts made by the nike+ iPhone application.
NIKEPLUS_CLIENT_ID     = <nikeplus-client-id>
NIKEPLUS_CLIENT_SECRET = <nikeplus-client-secret>
NIKEPLUS_APP           = <nikeplus-app>
```
* run unit tests which upload to nike+.
```
$ cat etc-test/test.properties
# Credentials for nike+ login (used by unit-tests).
nikeplus.email = <nikeplus-email>
nikeplus.password = <nikeplus-password>
```


Building
========
```
mvn package
```
