Development/Build Prerequisites
===============================
*nix system  
java sdk  
apache ant  


Directory Setup
===============
Name the directory containing this file "git" and put it in what you want to be the root of the project.  
 - In my setup this README is located at ~/projects/tcx2nikeplus/git/README.md

You may require the following property files, depending on what you want to do:

../etc/build.properties
if you wish to use the deploy.local ant target.  Example:

    # Location of various installed packages
    tomcat.dir      = /opt/local/share/tomcat-6.0.18

../etc/garmin.properties
if you want to download garmin activities.  Example:

    # Credentials for garmin connect login.
    GARMIN_USERNAME = <garmin-connect-username>
    GARMIN_PASSWORD = <garmin-connect-password>

../etc/nikeplus.properties
if you wish to upload to nike+.  I discovered these values by sniffing HTTP posts made by the nike+ iPhone application.

    # Credentials for nike+ login.
    NIKEPLUS_CLIENT_ID     = <nikeplus-client-id>
    NIKEPLUS_CLIENT_SECRET = <nikeplus-client-secret>
    NIKEPLUS_APP           = <nikeplus-app>


Building
========
From the project root run  
`ant -f git/build/build.xml`

This will build everything, you can call other ant targets if you want to be more specific.


Running
=======
After building, files are generated in dist/app/bin for conversion/upload on the command-line and a war is generated if you wish to deploy as a webapp.
