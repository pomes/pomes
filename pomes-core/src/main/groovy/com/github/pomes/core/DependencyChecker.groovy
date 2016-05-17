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

import groovy.util.logging.Slf4j
import org.apache.maven.artifact.Artifact
import org.owasp.dependencycheck.Engine
import org.owasp.dependencycheck.data.nvdcve.CveDB
import org.owasp.dependencycheck.data.nvdcve.DatabaseException
import org.owasp.dependencycheck.data.nvdcve.DatabaseProperties

import java.nio.file.Path

/**
 *
 *
 * @see <a href="https://github.com/jeremylong/DependencyCheck/blob/master/dependency-check-cli/src/main/java/org/owasp/dependencycheck/App.java">Dependency checker</a>
 */
@Slf4j
class DependencyChecker {

    static checkDependency(Artifact artifact, Path db, String outputFormat = 'html') {
        Engine engine
        CveDB cve
        DatabaseProperties prop
        try {
            engine = new Engine()
            try {
                cve = new CveDB()
                cve.open()
                prop = cve.getDatabaseProperties()
            } catch (DatabaseException ex) {
                log.debug "Unable to retrieve DB Properties", ex
            } finally {
                cve?.close()
            }
        } catch (DatabaseException ex) {
            log.error 'Unable to connect to the dependency-check database; analysis has stopped'
            log.debug("", ex);
        } finally {
            engine?.cleanup()
        }
    }

}
