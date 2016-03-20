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

@Parameters(commandNames = ['search'], commandDescription = "Searches for a library using search text or coordinates")
class CommandSearch {

    @Parameter(description = '<search text>')
    List<String> queryText

    @Parameter(names = ['-g', '--groupId'], description = 'Group ID')
    String groupId

    @Parameter(names = ['-a', '--artifactId'], description = 'Artifact ID')
    String artifactId

    final String usage = 'Search text or coordinates (group and/or artifact ID) must be provided'

    boolean isValid() {
        (queryText || (groupId || artifactId))
    }

    String getQueryText() {
        queryText.join ' '
    }

    def performSearch() {
        if (queryText) {
            println "Searching for: $queryText"
        } else {
            println "Searching for: $groupId:$artifactId"
        }
    }
}
