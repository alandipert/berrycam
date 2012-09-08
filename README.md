# BerryCam

A small Clojure webcam server for the Raspberry Pi.

[See a live demo!](http://pi.tailrecursion.com/)

## Usage

* Use the [Soft-float Debian “wheezy” SD card image](http://www.raspberrypi.org/downloads)
* Install Java JDK, Maven, and ant
* [Install v4l4j](http://code.google.com/p/v4l4j/wiki/GettingStartedOnRPi)
* Plug in your webcam
* Run in same directory as v4l4j.jar: `mvn install:install-file -Dfile=v4l4j.jar -DgroupId=au.edu.jcu -DartifactId=v4l4j -Dversion=0.9.0 -Dpackaging=jar`
* Run `lein deps`
* `script/run` to serve resources/htdocs on port 8080.

## License

Copyright © 2012 Alan Dipert

Distributed under the Eclipse Public License, the same as Clojure.
