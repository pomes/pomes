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

import com.github.pomes.cli.command.Command
import com.github.pomes.cli.command.CommandAbout
import com.github.pomes.cli.command.CommandGet
import com.github.pomes.cli.command.CommandHelp
import com.github.pomes.cli.command.CommandInfo
import com.github.pomes.cli.command.CommandQuery
import com.github.pomes.cli.command.CommandRepo
import com.github.pomes.cli.command.CommandSearch

enum CliCommands {
    HELP('help', CommandHelp),
    SEARCH('search', CommandSearch),
    GET('get', CommandGet),
    INFO('info', CommandInfo),
    QUERY('query', CommandQuery),
    REPO('repo', CommandRepo),
    ABOUT('about', CommandAbout)

    final String value
    final Command command

    CliCommands(String value, Class command) {
        this.value = value
        this.command = command.newInstance()
    }

    static CliCommands lookupCliCommand(String value) {
        for (CliCommands cmd in CliCommands.values()) {
            if (cmd.value == value) return cmd
        }
        return null
    }

    @Override
    public String toString() {
        return value
    }
}
