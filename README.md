# Client Skeletons in Java

This project provides client skeletons for the Arrowhead Framework Generation 4.0

These skeletons are to be extended with your own application code.

Contact: hegeduscs'at'aitia.ai

### Project structure

This is a multi module maven project, the
[root `pom.xml`](https://github.com/arrowhead-f/client-java/blob/master/pom.xml) lists all
the modules and common dependencies.

* **client-common**: a common library module for the other maven modules. Contains all the
  data transfer objects and common dependencies.

_Demos have been moved to the
[System of Systems](https://github.com/arrowhead-f/sos-examples) repository._

### Application Arguments

The Arrowhead systems built against the new ArrowheadClient (that includes the consumer,
provider, publisher, and subscriber demos) have the following arguments:

* **-daemon**: Start in daemon mode. In this mode the application will only terminate on the
  TERM signal, otherwise you can type `stop` to shutdown the process.
* **-d**: Start in debug mode.
* **-DconfDir=\<dir\>**: Location of `default.conf` and `app.conf` files, relative to the
  current working directory.Note this is a Java VM option.

The remain settings (that includes the previous `-tls` argument) are set in the
configuration files `default.conf` and `app.conf`.

### Config files

_Incomplete_

* **port=0**: Enable automatic port detection.
