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
import com.github.pomes.cli.Context
import com.github.pomes.cli.utility.MessageBundle
import com.github.pomes.core.query.RepositoryWebQueryResult
import groovy.util.logging.Slf4j

@Slf4j
@Parameters(commandNames = ['search'], resourceBundle = 'com.github.pomes.cli.MessageBundle', commandDescriptionKey = 'commandDescriptionSearch')
class CommandSearch implements Command {
    MessageBundle bundle = new MessageBundle(ResourceBundle.getBundle('com.github.pomes.cli.MessageBundle'))

    @Parameter(descriptionKey = 'parameterSearchText')
    List<String> queryText

    @Parameter(names = ['-g', '--groupId'], descriptionKey = 'parameterGroupId')
    String groupId

    @Parameter(names = ['-a', '--artifactId'], descriptionKey = 'parameterArtifactId')
    String artifactId

    boolean isValid() {
        (queryText || (groupId || artifactId))
    }

    String getQueryText() {
        queryText.join ' '
    }

    @Override
    Node handleRequest(Context context) {
        Node response = new Node(null, 'search')
        if (!isValid()) {
            new Node(response, 'error', [message: bundle.getString('error.commandSearchIllegalArgument')])
            return response
        }

        String query
        List<RepositoryWebQueryResult> searchResults
        if (queryText) {
            query = queryText
            searchResults = context.searcher.search(queryText)
            log.debug bundle.getString('log.searchResultCount', queryText, searchResults.size())
        } else {
            query = "g:$groupId a:$artifactId"
            searchResults = context.searcher.search(groupId, artifactId)
            log.debug bundle.getString('log.searchResultCount', "$groupId:$artifactId", searchResults.size())
        }

        response.append new NodeBuilder().results (
                count: searchResults.size(), query: query,
                provider: context.searcher.primarySearchProvider.displayName,
                providerApi: context.searcher.primarySearchProvider.apiUrl) {
            searchResults.each { searchResult ->
                result artifactId: searchResult.artifactId,
                        groupId: searchResult.groupId,
                        latestVersion: searchResult.latestVersion,
                        versions: searchResult.versions,
                        searchResult.description

            }
        }
        return response
    }
}
