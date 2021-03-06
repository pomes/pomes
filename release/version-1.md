# Release: version-1

This is the initial release and will provide baseline functionality, including:

* Definition of the `pomes-core` library to provide access to Maven repositories
* The `pomes-cli` library to:
    * Supply the command-line interface for searching and investigating
    * Generate various output formats (text, xml, html, json)

A large part of the work has also been to:

* Setup the distribution repositories in Bintray and JFrog's OSS repo
* Configuring the release system (a Gradle plugin)

## Known issues
There are lots - it's only early days! Check the GitHub issues for the project.

* [Projects with parent POMs appear to cause errors](https://github.com/pomes/pomes/issues/10)
