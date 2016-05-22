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

package com.github.pomes.core.repositories

import com.github.pomes.core.query.RepositoryWebQueryResult
import groovy.json.JsonSlurper
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.eclipse.aether.repository.RemoteRepository

import static javax.ws.rs.client.ClientBuilder.newClient
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE

/**
 * Describes the JCenter repository in Bintray
 *
 * @see <a href="https://bintray.com/docs/api/#_maven_package_search">Bintray API</a>
 */
@Slf4j
@ToString
final class MavenCentral implements RepositorySearcher {
    static final String displayName = 'Maven Central'
    static final String id = 'central'
    static final URL url = 'https://repo1.maven.org/maven2/'.toURL()
    static final String repositoryType = 'default'
    static final URL apiUrl = 'http://search.maven.org/solrsearch/select'.toURL()

    static RemoteRepository newMavenCentralRemoteRepository() {
        RemoteRepository.Builder builder = new RemoteRepository.Builder(id, repositoryType, url.toString())
        builder.build()
    }

    @Override
    List<RepositoryWebQueryResult> query(String query) {
        def json =
                newClient().
                        target(apiUrl.toURI()).
                        queryParam('q', "$query").
                        queryParam('wt', 'json').
                        request(APPLICATION_JSON_TYPE).get(String)
        log.debug "$displayName result for $query: $json"
        JsonSlurper slurper = new JsonSlurper()
        mapQueryResults slurper.parseText(json)
    }

    @Override
    List<RepositoryWebQueryResult> query(String groupId, String artifactId) {
        String operator = ''
        if (groupId && artifactId)
            operator = '+AND+'
        def json =
                newClient().
                        target(apiUrl.toURI()).
                        queryParam('q', "g:\"$groupId\"${operator}a:\"$artifactId\"").
                        queryParam('wt', 'json').
                        request(APPLICATION_JSON_TYPE).get(String)
        log.debug "$displayName result for $groupId:$artifactId: $json"
        def slurper = new JsonSlurper()
        mapQueryResults slurper.parseText(json)
    }

    private List<RepositoryWebQueryResult> mapQueryResults(List queryResults) {
        List<RepositoryWebQueryResult> results = []
        queryResults.each { Map result ->
            log.debug "Adding result: ${result.id}"
            def coordinates = result.id.tokenize(':')
            results << new RepositoryWebQueryResult(
                    groupId: result.g,
                    artifactId: result.a,
                    description: '',
                    versions: '',
                    latestVersion: result.v)
        }
        return results
    }

    @Override
    String getId() { MavenCentral.id }

    @Override
    String getDisplayName() { MavenCentral.displayName }

    @Override
    URL getApiUrl() { MavenCentral.apiUrl }

    @Override
    RepositorySearcher copy() {
        return new MavenCentral()
    }

}
