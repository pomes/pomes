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

package com.github.pomes.core

import com.github.pomes.core.query.RepositoryWebQueryResult
import com.github.pomes.core.repositories.RepositorySearcher
import groovy.util.logging.Slf4j

/**
 * Query tool for performing web queries against one or more maven repositories.
 *
 */
@Slf4j
class Searcher {
    final RepositorySearcher primarySearchProvider
    final List<RepositorySearcher> alternativeSearchProviders

    Searcher(RepositorySearcher primary, RepositorySearcher... alternatives = []) {
        this.primarySearchProvider = primary.copy()
        this.alternativeSearchProviders = alternatives*.copy()
    }

    List<RepositoryWebQueryResult> search(String query) {
        if (primarySearchProvider) {
            log.debug "Performing search: $query"
            return primarySearchProvider.query(query)
        } else {
            log.debug 'Cannot perform search as primarySearchProvider is null'
            return []
        }
    }

    List<RepositoryWebQueryResult> search(String groupId, String artifactId) {
        if (primarySearchProvider) {
            log.debug "Performing search: $groupId:$artifactId"
            return primarySearchProvider.query(groupId, artifactId)
        } else {
            log.debug 'Cannot perform search as primarySearchProvider is null'
            return []
        }
    }

    @Override
    String toString() {
        "Primary: $primarySearchProvider"
    }
}
