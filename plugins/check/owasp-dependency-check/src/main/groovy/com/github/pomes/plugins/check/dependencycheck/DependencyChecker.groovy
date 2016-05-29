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

import Slf4j
import Artifact
import org.owasp.dependencycheck.Engine
import org.owasp.dependencycheck.data.nvdcve.CveDB
import org.owasp.dependencycheck.data.nvdcve.DatabaseException
import org.owasp.dependencycheck.data.nvdcve.DatabaseProperties
import org.owasp.dependencycheck.dependency.Dependency
import org.owasp.dependencycheck.reporting.ReportGenerator

import javax.json.Json
import java.nio.file.Path

/**
 *
 *
 * @see <a href="https://github.com/jeremylong/DependencyCheck/blob/master/dependency-check-cli/src/main/java/org/owasp/dependencycheck/App.java">Dependency checker</a>
 */
@Slf4j
class DependencyChecker {

    static String checkDependency(Path db, String applicationName, Artifact... artifacts) {
        Engine engine
        CveDB cve
        DatabaseProperties databaseProperties

        try {
            com.github.pomes.core.DependencyChecker.log.info "Scanning artifacts: ${artifacts*.file}"
            engine = new Engine()
            engine.scan(artifacts*.file)
            engine.analyzeDependencies()
            final List<Dependency> dependencies = engine.getDependencies()
            try {
                cve = new CveDB()
                cve.open()
                databaseProperties = cve.getDatabaseProperties()
                final ReportGenerator report = new ReportGenerator(applicationName, dependencies, engine.analyzers, databaseProperties)


            } catch (DatabaseException ex) {
                com.github.pomes.core.DependencyChecker.log.debug "Unable to retrieve DB Properties", ex
            } finally {
                cve?.close()
            }
        } catch (DatabaseException ex) {
            com.github.pomes.core.DependencyChecker.log.error 'Unable to connect to the dependency-check database; analysis has stopped'
            com.github.pomes.core.DependencyChecker.log.debug("", ex);
        } finally {
            engine?.cleanup()
        }
    }

}
