# Pomes

[![Build Status](https://travis-ci.org/pomes/pomes.svg?branch=master)](https://travis-ci.org/pomes/pomes)

![Pomes logo - a pomegranate](https://github.com/pomes/pomes/blob/master/resources/logo/pomegranate-300px.png)

Pomes is a library and command-line tool for searching Maven-based repositories and otherwise working with
Java libraries utilising the Maven POM approach to distribution.

I'm still working towards the first release so watch this space.

## Releases
Pomes is released in two ways:

* As an installable distribution:
    * [Bintray](https://bintray.com/pomes/Release)
    * [GitHub](https://github.com/pomes/pomes/releases)
* As a maven library:
    * Through [Bintray](https://bintray.com/pomes/pomes)

For release notes, please refer to the [release directory](https://github.com/pomes/pomes/tree/master/release)

### Snapshots
I publish snapshots to [JFrog's OSS repository](https://oss.jfrog.org/webapp/#/artifacts/browse/simple/General/oss-snapshot-local/com/github/pomes)

Thanks to [JFrog](https://www.jfrog.com/) for making this service freely available to open source projects.

# Acknowledgements
Pomes makes use of a number of libraries, both directly and indirectly. I'd like to acknowledge a few of the larger
components:

* The [Maven project](http://maven.apache.org/) describes Maven-based packaging and repositories as well as libraries for working with them
* The [Aether project](http://www.eclipse.org/aether/) provides a good abstraction for handling Maven repositories
* The source for this project is written in [Groovy](http://groovy-lang.org/) and uses the [Gradle build tool](http://gradle.org/)
* I've been using the [JCenter repository](http://jcenter.bintray.com/), provided by JFrog Bintray, as the primary repository for searching and access

The Gradle build used by this project will advise you of the full suite of dependencies -
just run `./gradlew dependencies`. The [Gradle documentation](https://docs.gradle.org/current/userguide/tutorial_gradle_command_line.html#sec:listing_dependencies)
provides a description of this command.
