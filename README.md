fclib
=====

Library for connecting to repositories (Fedora Commons and Webdav); also a tool for generating FOXML for TIFF files

This library contains code for dealing with a Fedora Commons instance (3.6.2 tested)

*DataConnector* contains code to get and post objects from/to a Fedora Commons instance.  Settings are loaded from properties files in the resources folder.

*FedoraIngestXMLGenerator* contains code to generate a FOXML file for ingesting a TIFF file into FC.  It creates appropriate datastreams and a basic SCAPE METS.

NOTE: there are methods/abstractions that could be better shared between classes but I don't intend to do further work on this at the moment.

