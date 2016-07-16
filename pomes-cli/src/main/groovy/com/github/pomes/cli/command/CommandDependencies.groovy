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
import org.eclipse.aether.graph.Dependency

import static com.github.pomes.cli.command.CommandUtil.*

@Slf4j
@Parameters(commandNames = ['dependencies'],
        resourceBundle = 'com.github.pomes.cli.MessageBundle',
        commandDescriptionKey = 'commandDescriptionDependencies')
class CommandDependencies implements Command {
    static final String NODE_DEPENDENCIES = 'dependencies'
    static final String NODE_DEPENDENCY = 'dependency'

    @Parameter(descriptionKey = 'parameterCoordinates')
    List<String> coordinates

    @Parameter(names = ['-l', '--latest'], descriptionKey = 'parameterLatest')
    Boolean latest

    //@Parameter(names = ['-s', '--scope'], description = 'parameterScope')
    //String scope = COMPILE

    @Override
    Node handleRequest(Context context) {
        MessageBundle bundle = context.app.bundle
        Resolver resolver = context.resolver
        Node response = new Node(null, NODE_DEPENDENCIES)
        Node coordinatesNode = new Node(response, NODE_COORDINATES)
        coordinates.each { coordinate ->
            Node coordinateNode = new Node(coordinatesNode, NODE_COORDINATES, [name: coordinate])
            Node dependenciesNode = new Node(coordinateNode, NODE_DEPENDENCIES)
            log.info bundle.getString('log.commandRequest', NODE_DEPENDENCIES, coordinate, latest)

            ArtifactCoordinate ac = ArtifactCoordinate.parseCoordinates(coordinate)

            if (latest) {
                ac = ac.copyWith(version: resolver.getArtifactLatestVersion(ac))
            }

            /*
            CollectResult collectResult = resolver.collectAllDependencies(ac.artifact, scope)
            log.debug bundle.getString('log.dependencyRoot', 'info', collectResult)
            new Node(response,'collectResult', collectResult)
            */

            List<Dependency> dependencyListing = resolver.getDirectDependencies(ac.artifact)
            dependencyListing.each { dep ->
                new Node(dependenciesNode, NODE_DEPENDENCY, [
                        name      : dep.artifact.toString(),
                        artifactId: dep.artifact.artifactId,
                        groupId   : dep.artifact.groupId,
                        version   : dep.artifact.version,
                        scope     : dep.scope,
                        classifier: dep.artifact.classifier,
                        extension : dep.artifact.extension,
                        optional  : dep.optional])
            }
        }
        return response
    }
}
