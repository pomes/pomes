package com.github.pomes.cli

import com.beust.jcommander.JCommander
import com.beust.jcommander.MissingCommandException
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException
import com.github.pomes.cli.command.Command
import com.github.pomes.core.Resolver
import com.github.pomes.core.Searcher
import com.github.pomes.core.repositories.DefaultLocalRepository
import com.github.pomes.core.repositories.JCenter
import groovy.util.logging.Slf4j

import java.nio.file.Files

/*
 *    Copyright 2016 Duncan Dickinson
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

@Slf4j
class Cli {

    static final String programVersion = '0.1.0'
    static final String programName = 'pomes'

    final Searcher searcher
    final Resolver resolver

    @Parameter(names = ['-h', '--help'], help = true)
    Boolean help

    @Parameter(names = ['-v', '--version'], description = 'Displays version information')
    Boolean version

    JCommander jc

    Cli() {
        jc = new JCommander(this)
        searcher = new Searcher(new JCenter())
        resolver = new Resolver([JCenter.newJCenterRemoteRepository()], DefaultLocalRepository.repository)

        if (!Files.exists(DefaultLocalRepository.baseDir)) {
            Files.createDirectories(DefaultLocalRepository.baseDir)
        }

        jc.programName = 'pomes'
        CliCommands.values().each { cmd ->
            jc.addCommand cmd.command
        }
    }

    Boolean parse(String... args) {
        try {
            jc.parse(args)
            return true
        } catch (MissingCommandException mce) {
            log.error "Missing command ($args) - $mce.message"
            System.err.println "Expected a command (${CliCommands.values().value}"
            return false
        } catch (ParameterException pe) {
            log.error "Parameter exception ($args) - $pe.message"
            System.err.println pe.message
            return false
        }
    }

    String getCommand() { jc.parsedCommand }

    void handleRequest() {
        if (version) {
            println "$programName $programVersion"
            return
        }

        if (help) {
            jc.usage()
            return
        }

        Command cmd = CliCommands.lookupCliCommand(command).command

        if (cmd) {
            log.debug "Calling command: $cmd"
            cmd.handleRequest(searcher, resolver)
            return
        } else {
            jc.usage()
            return
        }
    }

    static void main(String[] args) {
        Cli cli = new Cli()

        if (cli.parse(args)) {
            cli.handleRequest()
        } else {
            System.exit(-1)
        }
    }
}
