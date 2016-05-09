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
import com.github.pomes.core.ArtifactCoordinate
import com.github.pomes.core.Resolver
import com.github.pomes.core.Searcher
import groovy.text.GStringTemplateEngine
import groovy.util.logging.Slf4j
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.graph.Dependency

@Slf4j
@Parameters(commandNames = ['check'], commandDescription = "Performs checks on an artifact")
class CommandCheck implements Command {
    @Parameter(description = '<coordinates>')
    List<String> coordinates

    @Parameter(names = ['-l', '--latest'], description = 'Use the latest version')
    Boolean latest

    @Override
    void handleRequest(Searcher searcher, Resolver resolver) {
        coordinates.each { coordinate ->
            log.debug "Check request for $coordinate (latest requested: $latest)"

            ArtifactCoordinate ac = ArtifactCoordinate.parseCoordinates(coordinate)
            String latestVersion = resolver.getArtifactLatestVersion(ac)

            if (latest) {
                ac = ac.copyWith(version: latestVersion)
            }

            Artifact artifact = resolver.getArtifact(ac).artifact

            Map<Dependency> outdatedDependencyMap = [:]

            resolver.getDirectDependencies(artifact)?.each { Dependency dependency ->
                String latest = resolver.getArtifactLatestVersion(dependency.artifact)
                if (latest != dependency.artifact.version) {
                    outdatedDependencyMap << ["$dependency.artifact": [
                            latestVersion: latest,
                            scope: dependency.scope,
                            dependency: dependency]
                    ]
                    println " - ${"[${dependency.scope}]".padRight(12)} $dependency.artifact $latest"
                }
            }

            URL template = this.class.getResource('/com/github/pomes/cli/templates/model/check.txt')

            if (template) {
                GStringTemplateEngine engine = new GStringTemplateEngine()

                println engine.createTemplate(template)
                        .make([artifact    : artifact,
                               latestVersion: latestVersion,
                               outdatedDependencies: outdatedDependencyMap])
                        .toString()
            } else {
                System.err.println "Failed to load the requested template"
                System.exit(-1)
            }
        }
    }
}
