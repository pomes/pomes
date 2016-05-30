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
import com.github.pomes.core.ArtifactCoordinate
import com.github.pomes.core.Resolver
import groovy.util.logging.Slf4j
import org.apache.maven.model.Model
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.graph.Dependency

@Slf4j
@Parameters(commandNames = ['check'], resourceBundle = 'com.github.pomes.cli.MessageBundle', commandDescriptionKey = "commandDescriptionCheck")
class CommandCheck implements Command {
    MessageBundle bundle = new MessageBundle(ResourceBundle.getBundle('com.github.pomes.cli.MessageBundle'))

    @Parameter(descriptionKey = 'parameterCoordinates')
    List<String> coordinates

    @Parameter(names = ['-l', '--latest'], descriptionKey = 'parameterLatest')
    Boolean latest

    //@Parameter(names = ['-t', '--transitive'], description = 'parameterTransitive')
    //Boolean transitive

    //@Parameter(names = ['-s', '--scope'], description = 'parameterScope')
    //String scope

    @Override
    Node handleRequest(Context context) {
        Resolver resolver = context.resolver
        Node response = new Node(null, 'check')
        Node coordinatesNode = new Node(response, 'coordinates')
        coordinates.each { coordinate ->
            Node coordinateNode = new Node(coordinatesNode, 'coordinate', [name: coordinate])
            log.info bundle.getString('log.commandRequest', 'check', coordinate, latest)

            ArtifactCoordinate ac = ArtifactCoordinate.parseCoordinates(coordinate)
            String latestVersion = resolver.getArtifactLatestVersion(ac)

            if (latest) {
                ac = ac.copyWith(version: latestVersion)
            }

            Artifact artifact = resolver.getArtifact(ac).artifact
            Node outdatedDependencies = new Node(coordinateNode, 'outdatedDependencies')
            resolver.getDirectDependencies(artifact)?.each { Dependency dependency ->
                String latest = resolver.getArtifactLatestVersion(dependency.artifact)
                if (latest != dependency.artifact.version) {
                    new Node(outdatedDependencies, 'dependency', [
                            latestVersion: latest,
                            name         : dependency.artifact.toString(),
                            artifactId   : dependency.artifact.artifactId,
                            groupId      : dependency.artifact.groupId,
                            version      : dependency.artifact.version,
                            scope        : dependency.scope,
                            classifier   : dependency.artifact.classifier,
                            extension    : dependency.artifact.extension,
                            optional     : dependency.optional])
                }
            }

            //Model model = resolver.getEffectiveModel(artifact)

            /*
            //TODO: Mainly need this if given a POM - not needed(?) if coordinate includes jar
            ArtifactCoordinate packageAC = new ArtifactCoordinate(groupId: model.groupId,
                    artifactId: model.artifactId,
                    version: model.version,
                    extension: model.packaging)
            Artifact packageArtifact = resolver.getArtifact(packageAC).artifact
            */
            /*
            TODO: Re-enable once node work is complete
            //Map<Dependency> outdatedDependencyMap = [:]
            //CollectResult collectResult
            //DependencyVisitor visitor
            if (transitive) {
                collectResult = resolver.collectAllDependencies(ac.artifact, scope)
                log.debug bundle.getString('log.dependencyRoot', 'info', collectResult.root.artifact)
                //visitor = new CommandLineDumperTransitiveDependencyCheck(resolver)
                new Node(response,'collectResult', collectResult)
            } else {
            */

            /*
            response.append new NodeBuilder().outdatedDependencies {
                resolver.getDirectDependencies(artifact)?.
                        findAll { scope && (it.scope != scope) }?.
                        each { Dependency dependency ->
                            String latest = resolver.getArtifactLatestVersion(dependency.artifact)
                            if (latest != dependency.artifact.version) {
                                "$dependency.artifact"(
                                        latestVersion: latest,
                                        scope: dependency.scope,
                                        dependency: dependency)
                            }
                        }
            }
            */
            /*
            TODO: Remove
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
            */
        }
        return response
    }
}
