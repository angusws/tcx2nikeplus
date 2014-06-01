Development/Build Prerequisites
===============================
*nix system  
java sdk  
maven 3


Directory Setup
===============
Name the directory containing this file "git" and put it in what you want to be the root of the project.  
 - In my setup this README is located at ~/projects/tcx2nikeplus/git/README.md

You may require the following property files, depending on what you want to do:

etc/garmin.properties
if you want to download garmin activities.  Example:

    # Credentials for garmin connect login.
    GARMIN_USERNAME = <garmin-connect-username>
    GARMIN_PASSWORD = <garmin-connect-password>

etc/nikeplus.properties
if you wish to upload to nike+.  I discovered these values by sniffing HTTP posts made by the nike+ iPhone application.

    # Credentials for nike+ login.
    NIKEPLUS_CLIENT_ID     = <nikeplus-client-id>
    NIKEPLUS_CLIENT_SECRET = <nikeplus-client-secret>
    NIKEPLUS_APP           = <nikeplus-app>


Building
========
From the project root run  
mvn package
