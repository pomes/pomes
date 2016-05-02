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

/**
 * Basic interface for searching a repository
 */
interface RepositorySearcher {
    /**
     * Performs a general search. No search syntax should be added by implementing classes
     * @param query the search text
     * @return a list of 0..n results
     */
    List<RepositoryWebQueryResult> query(String query)

    /**
     * Performs a search based on group and/or artifact coordinates
     * @param groupId
     * @param artifactId
     * @return a list of 0..n results
     */
    List<RepositoryWebQueryResult> query(String groupId, String artifactId)

    /**
     * Cloner
     * @return a new instance that matches the current
     */
    RepositorySearcher copy()
}
