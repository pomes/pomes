package com.github.pomes.cli.command

import com.beust.jcommander.Parameters
import com.github.pomes.cli.Cli
import com.github.pomes.core.Resolver
import com.github.pomes.core.Searcher
import groovy.util.logging.Slf4j

@Slf4j
@Parameters(commandNames = ['about'], commandDescription = "Displays program information")
class CommandAbout implements Command {
    @Override
    void handleRequest(Searcher searcher, Resolver resolver) {
        println "$Cli.programName v$Cli.programVersion"
        println 'Licensed under: Apache License Version 2.0, January 2004'
        //TODO: Display info of 3rd party libs
    }
}
