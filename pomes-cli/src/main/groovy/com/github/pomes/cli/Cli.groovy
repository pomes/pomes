package com.github.pomes.cli

import com.beust.jcommander.JCommander
import com.beust.jcommander.MissingCommandException
import com.beust.jcommander.Parameter
import com.github.pomes.cli.command.CommandGet
import com.github.pomes.cli.command.CommandHelp
import com.github.pomes.cli.command.CommandInfo
import com.github.pomes.cli.command.CommandRepo
import com.github.pomes.cli.command.CommandSearch
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

    final Searcher searcher
    final Resolver resolver

    @Parameter(names = ['-h', '--help'], help = true)
    Boolean help = false

    final JCommander jc

    def commandHelp = new CommandHelp()
    def commandSearch = new CommandSearch()
    def commandRepo = new CommandRepo()
    def commandGet = new CommandGet()
    def commandInfo = new CommandInfo()

    Cli(){
        searcher = new Searcher(new JCenter())

        if (! Files.exists(DefaultLocalRepository.baseDir)) {
            Files.createDirectories(DefaultLocalRepository.baseDir)
        }

        resolver = new Resolver([JCenter.newJCenterRemoteRepository()], DefaultLocalRepository.repository)

        jc = new JCommander(this)
        jc.programName = 'pomes'
        jc.addCommand commandHelp
        jc.addCommand commandSearch
        jc.addCommand commandInfo
        jc.addCommand commandGet
        jc.addCommand commandRepo
    }

    Boolean parse(String... args) {
        try {
            jc.parse(args)
        } catch (MissingCommandException mce) {
            return false
        }
        true
    }

    String getCommand() { jc.parsedCommand }

    /*
    StringBuilder getUsage() {
        StringBuilder out = new StringBuilder()
        jc.usage(out)
        out
    }
    */

    void getCommandUsage(String command = '', StringBuilder out) {
        out.append getCommandUsage(command)
    }

    StringBuilder getCommandUsage(String command = '') {
        StringBuilder out = new StringBuilder()
        if (command && jc.commands.containsKey(command)) {
            jc.usage(command, out)
        } else {
            out << 'Please select a command:\n'
            jc.commands.findAll { it.key != CliCommands.HELP.name }.each { cmd, cmdObj ->
                out << "  $cmd: ${jc.getCommandDescription(cmd)}\n"
            }
        }
        out
    }

    void handleRequest(Searcher searcher, Resolver resolver) {
        if (!command || !jc.commands.containsKey(command)) {
            jc.usage()
        } else {
            log.debug "Calling command: $command"
            switch (command) {
                case CliCommands.HELP.name:
                    println getCommandUsage(commandHelp.helpSubCommands?.get(0))
                    break
                case CliCommands.SEARCH.name:
                    if (commandSearch.isValid())
                        commandSearch.handleRequest(searcher, resolver)
                    else
                        println "Incorrect usage: ${commandSearch.usage}"
                    break
                case CliCommands.INFO.name:
                    //commandInfo.handleRequest(searcher, resolver)
                    println 'info is not yet implemented'
                    break
                case CliCommands.GET.name:
                    commandGet.handleRequest(searcher, resolver)
                    break
                case CliCommands.REPO.name:
                    println 'repo is not yet implemented'
                    break
                default:
                    println getCommandUsage(commandHelp.helpSubCommands?.get(0))
            }
        }
    }

    static void main(String[] args) {
        Cli cli = new Cli()
        log.debug "Searcher: $cli.searcher"

        if (cli.parse(args))
            cli.handleRequest(cli.searcher, cli.resolver)
        else
            println cli.commandUsage
    }
}
