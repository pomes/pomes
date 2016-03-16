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

package com.github.pomes.cli

import com.beust.jcommander.JCommander
import com.beust.jcommander.MissingCommandException
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.github.pomes.cli.command.*

@Parameters(separators = "=")
class CliArgs {
    @Parameter(names = ['-h', '--help'], help = true)
    Boolean help = false

    final JCommander jc

    def commandHelp = new CommandHelp()
    def commandSearch = new CommandSearch()
    def commandRepo = new CommandRepo()

    CliArgs() {
        jc = new JCommander(this)
        jc.programName = 'pomes'
        jc.addCommand commandHelp
        jc.addCommand commandSearch
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

    StringBuilder getUsage() {
        StringBuilder out = new StringBuilder()
        jc.usage(out)
        out
    }

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

    final Map<String, Closure> commandHandlers = [
            search: {
                if (commandSearch.isValid())
                    commandSearch.performSearch()
                else
                    println "Incorrect usage: ${commandSearch.usage}"
            },
            help  : {
                println getCommandUsage(commandHelp.helpSubCommands?.get(0))
            }
    ]

    void handleRequest() {
        if (!command || !jc.commands.containsKey(command))
            jc.usage()
        else
            commandHandlers."$command"()
    }
}
