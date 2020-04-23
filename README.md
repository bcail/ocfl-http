# ocfl-http

HTTP interface for an OCFL repository, using [ocfl-java][] for reading and writing the OCFL data.

Note: this is a side-project that is not being used in production.

[ocfl-java]: https://github.com/UW-Madison-Library/ocfl-java

## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein ring server OR lein run

To create a jar file, run:

    lein uberjar

To run the jar file, run:

    java -jar target/ocfl-http.jar

