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

import co.freeside.betamax.Betamax
import co.freeside.betamax.Recorder
import co.freeside.betamax.TapeMode
import com.github.pomes.core.RepositoryWebQueryResult
import groovy.util.logging.Slf4j
import org.eclipse.aether.repository.RemoteRepository
import org.junit.Rule
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

import static com.github.pomes.core.repositories.JCenter.newJCenterRemoteRepository

@Unroll
@Slf4j
class JCenterTest extends Specification {
    def configureRecorder = { ->
        def rec = new Recorder()
        rec.sslSupport = true
        return rec
    }

    @Rule
    Recorder recorder = configureRecorder()

    def "Ensure that the JCenter repository is correctly defined"() {
        when:
        RemoteRepository repo = newJCenterRemoteRepository()
        then:
        repo.id == 'jcenter'
        repo.url == 'http://jcenter.bintray.com/'

    }

    @Ignore
    @Betamax(tape = 'jCenterKeywordQuery.tape', mode = TapeMode.READ_WRITE)
    def "Test basic keyword query: #query"() {
        given:
        JCenter repo = new JCenter()
        when:
        List<RepositoryWebQueryResult> results = repo.query(query)
        then:
        println results.size()
        where:
        query      || result
        '*groovy*' || _
    }

}
