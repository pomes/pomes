package com.github.pomes.cli.command

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

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.github.pomes.cli.Context
import com.github.pomes.cli.utility.MessageBundle
import com.github.pomes.core.ArtifactCoordinate
import com.github.pomes.core.Resolver
import groovy.util.logging.Slf4j
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.graph.Dependency

@Slf4j
@Parameters(commandNames = ['check'],
        resourceBundle = 'com.github.pomes.cli.MessageBundle',
        commandDescriptionKey = 'commandDescriptionCheck')
class CommandCheck implements Command {
    static final String NODE_CHECK = 'check'

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
        MessageBundle bundle = context.app.bundle
        Resolver resolver = context.resolver
        Node response = new Node(null, NODE_CHECK)
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
        }
        response
    }
}
