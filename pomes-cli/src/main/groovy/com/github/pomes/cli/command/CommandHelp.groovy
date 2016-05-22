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

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.github.pomes.cli.CliCommands
import com.github.pomes.cli.Context

@Parameters(commandNames = ['help'], resourceBundle = 'com.github.pomes.cli.MessageBundle', commandDescriptionKey = 'commandDescriptionHelp')
class CommandHelp implements Command {
    @Parameter(descriptionKey = 'parameterSubCommand')
    List<String> helpSubCommands = []

    @Override
    Node handleRequest(Context context) {
        Node response = new Node(null, 'help')
        if (helpSubCommands) {
            helpSubCommands.each {
                StringBuilder out = new StringBuilder()
                context.jCommander.usage(it, out)
                new Node(response, it, out.toString())
            }
        } else {
            context.jCommander.commands.each { cmdObj ->
                //cmdObj.key(context.jCommander.getCommandDescription(cmdObj.key))
                new Node(response, cmdObj.key, context.jCommander.getCommandDescription(cmdObj.key))
            }
        }
        return response
    }
}
