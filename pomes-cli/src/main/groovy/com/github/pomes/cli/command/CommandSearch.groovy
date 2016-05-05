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
import com.github.pomes.core.Resolver
import com.github.pomes.core.Searcher
import com.github.pomes.core.query.RepositoryWebQueryResult
import groovy.util.logging.Slf4j

@Slf4j
@Parameters(commandNames = ['search'], commandDescription = "(Web) Searches for an artifact using search text or coordinates")
class CommandSearch implements Command {

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

    @Override
    void handleRequest(Searcher searcher, Resolver resolver) {
        if (!isValid()) {
            throw new IllegalArgumentException("Incorrect usage: ${commandSearch.usage}")
        }

        List<RepositoryWebQueryResult> results
        if (queryText) {
            results = searcher.search(queryText)
            log.debug "Search for $queryText yielded ${results.size()} results"
        } else {
            results = searcher.search(groupId,artifactId)
            log.debug "Search for $groupId:$artifactId yielded ${results.size()} results"
        }

        if (results) {
            println "Result count: ${results.size()}"
            results.each {
                println " - ${it.toString()}"
            }
        } else {
            println "No results"
        }
    }
}
