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
import com.github.pomes.core.ArtifactExtension
import com.github.pomes.core.Resolver
import com.github.pomes.core.Searcher
import com.github.pomes.core.dependency.graph.display.CommandLineDumper
import groovy.util.logging.Slf4j
import org.apache.maven.model.Model
import org.eclipse.aether.collection.CollectResult
import org.eclipse.aether.graph.Dependency

import static org.eclipse.aether.util.artifact.JavaScopes.COMPILE

@Slf4j
@Parameters(commandNames = ['dependencies'], resourceBundle = 'com.github.pomes.cli.MessageBundle', commandDescriptionKey = 'commandDescriptionDependencies')
class CommandDependencies implements Command {

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
        Node response = new Node(null, 'dependencies')
        Node coordinatesNode = new Node(response, 'coordinates')
        coordinates.each { coordinate ->
            Node coordinateNode = new Node(coordinatesNode, 'coordinate', [name: coordinate])
            Node dependenciesNode = new Node(coordinateNode, 'dependencies')
            log.info bundle.getString('log.commandRequest', 'dependencies', coordinate, latest)

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
                new Node(dependenciesNode,'dependency',[
                        name: dep.artifact.toString(),
                        artifactId: dep.artifact.artifactId,
                        groupId: dep.artifact.groupId,
                        version: dep.artifact.version,
                        scope: dep.scope,
                        classifier: dep.artifact.classifier,
                        extension: dep.artifact.extension,
                        optional: dep.optional])
            }
        }
        return response
    }
}
