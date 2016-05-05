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

package com.github.pomes.cli.command

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.github.pomes.cli.CliCommands
import com.github.pomes.core.Resolver
import com.github.pomes.core.Searcher

@Parameters(commandNames = ['help'], commandDescription = "Displays help information for sub-commands")
class CommandHelp implements Command {
    @Parameter(description = 'The requested command(s) for which you seek help')
    List<String> helpSubCommands = []

    @Override
    void handleRequest(Searcher searcher, Resolver resolver) {
        /*
         * TODO: This is not efficient, it repeats the block in Cli
         */
        JCommander jc = new JCommander()
        CliCommands.values().each { cmd ->
            jc.addCommand cmd.command
        }
        // End of todo

        StringBuilder out = new StringBuilder()

        if (helpSubCommands[0]) {
            jc.usage(helpSubCommands[0], out)
        } else {
            out << 'Please select a command:\n'
            jc.commands.findAll { key, value -> key != CliCommands.HELP }.each { cmdObj ->
                out << "  ${"$cmdObj.key:".padRight(10)}${jc.getCommandDescription(cmdObj.key)} \n"
            }
        }
        println out
    }
}
