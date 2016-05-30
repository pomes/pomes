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
final class JCenter implements RepositorySearcher {
    static final String displayName = 'JCenter'
    static final String id = 'jcenter'
    static final URL url = 'http://jcenter.bintray.com/'.toURL()
    static final String repositoryType = 'default'
    static final URL apiUrl = 'https://api.bintray.com/search/packages/maven/'.toURL()

    static RemoteRepository newJCenterRemoteRepository() {
        RemoteRepository.Builder builder = new RemoteRepository.Builder(id, repositoryType, url.toString())
        builder.build()
    }

    @Override
    List<RepositoryWebQueryResult> query(String query) {
        def json =
                newClient().
                        target(apiUrl.toURI()).
                        queryParam('q', "$query").
                        request(APPLICATION_JSON_TYPE).get(String)
        log.debug "JCenter result for $query: $json"
        JsonSlurper slurper = new JsonSlurper()
        mapQueryResults slurper.parseText(json)
    }

    @Override
    List<RepositoryWebQueryResult> query(String groupId, String artifactId) {
        def json =
                newClient().
                        target(apiUrl.toURI()).
                        queryParam('g', "$groupId").
                        queryParam('a', "$artifactId").
                        request(APPLICATION_JSON_TYPE).get(String)
        log.debug "JCenter result for $groupId:$artifactId: $json"
        def slurper = new JsonSlurper()
        mapQueryResults slurper.parseText(json)
    }

    private List<RepositoryWebQueryResult> mapQueryResults(List queryResults) {
        List<RepositoryWebQueryResult> results = []
        queryResults.each { Map result ->
            log.debug "Adding result: ${result.system_ids[0]}"
            def coordinates = result.system_ids[0].tokenize(':')
            results << new RepositoryWebQueryResult(
                    groupId: coordinates[0],
                    artifactId: coordinates[1],
                    description: result.desc?.replaceAll(/(\r\n|\r|\n)/, ' '),
                    versions: result.versions,
                    latestVersion: result.latest_version)
        }
        return results
    }

    @Override
    String getId() { JCenter.id }

    @Override
    String getDisplayName() { JCenter.displayName }

    @Override
    URL getApiUrl() { JCenter.apiUrl }

    @Override
    RepositorySearcher copy() {
        return new JCenter()
    }

}
