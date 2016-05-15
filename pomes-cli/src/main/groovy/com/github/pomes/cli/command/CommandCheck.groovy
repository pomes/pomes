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
import com.github.pomes.core.dependency.graph.display.CommandLineDumperTransitiveDependencyCheck
import groovy.text.GStringTemplateEngine
import groovy.util.logging.Slf4j
import org.apache.maven.model.Model
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.collection.CollectResult
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.graph.DependencyVisitor

@Slf4j
@Parameters(commandNames = ['check'], commandDescription = "Performs checks on an artifact")
class CommandCheck implements Command {
    @Parameter(description = '<coordinates>')
    List<String> coordinates

    @Parameter(names = ['-l', '--latest'], description = 'Use the latest version')
    Boolean latest

    @Parameter(names = ['-t', '--transitive'], description = 'Check transitive dependencies')
    Boolean transitive

    @Parameter(names = ['-s', '--scope'], description = 'Sets the dependency scope')
    String scope

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
            Model model = resolver.getEffectiveModel(artifact)

            //TODO: Mainly need this if given a POM - not needed(?) if coordinate includes jar
            ArtifactCoordinate packageAC = new ArtifactCoordinate(groupId: model.groupId,
                    artifactId: model.artifactId,
                    version: model.version,
                    extension: model.packaging)
            Artifact packageArtifact = resolver.getArtifact(packageAC).artifact

            Map<Dependency> outdatedDependencyMap = [:]
            CollectResult collectResult
            DependencyVisitor visitor

            if (transitive) {
                collectResult = resolver.collectAllDependencies(ac.artifact, scope)
                log.debug "Dependency root: ${collectResult.root.artifact}"
                visitor = new CommandLineDumperTransitiveDependencyCheck(resolver)
            } else {
                resolver.getDirectDependencies(artifact)?.each { Dependency dependency ->
                    if (scope && dependency.scope != scope)
                        return
                    String latest = resolver.getArtifactLatestVersion(dependency.artifact)
                    if (latest != dependency.artifact.version) {
                        outdatedDependencyMap << ["$dependency.artifact": [
                                latestVersion: latest,
                                scope        : dependency.scope,
                                dependency   : dependency]
                        ]
                    }
                }
            }
            URL template = this.class.getResource('/com/github/pomes/cli/templates/model/check.txt')

            if (template) {
                GStringTemplateEngine engine = new GStringTemplateEngine()

                println engine.createTemplate(template)
                        .make([artifact                      : artifact,
                               packageArtifact               : packageArtifact,
                               model                         : model,
                               transitive                    : transitive,
                               latestVersion                 : latestVersion,
                               outdatedDependencies          : outdatedDependencyMap,
                               outdatedTransitiveDependencies: collectResult,
                               visitor                       : visitor])
                        .toString()
            } else {
                System.err.println "Failed to load the requested template"
                System.exit(-1)
            }
        }
    }
}
