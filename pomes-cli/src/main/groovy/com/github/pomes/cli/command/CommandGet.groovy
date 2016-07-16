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
import com.github.pomes.core.ArtifactClassifier
import com.github.pomes.core.ArtifactCoordinate
import com.github.pomes.core.ArtifactExtension
import com.github.pomes.core.Resolver
import com.github.pomes.core.Searcher
import groovy.util.logging.Slf4j
import org.eclipse.aether.resolution.ArtifactResolutionException
import org.eclipse.aether.resolution.ArtifactResult

import static com.github.pomes.cli.command.CommandUtil.*

@Slf4j
@Parameters(commandNames = ['get'],
        resourceBundle = 'com.github.pomes.cli.MessageBundle',
        commandDescriptionKey = 'commandDescriptionGet')
class CommandGet implements Command {

    @Parameter(descriptionKey = 'parameterCoordinates')
    List<String> coordinates

    @Parameter(names = ['-l', '--latest'], descriptionKey = 'parameterLatest')
    Boolean latest = false

    @Parameter(names = ['-e', '--extension'], descriptionKey = 'parameterExtension')
    String extension

    @Parameter(names = ['-c', '--classifier'], descriptionKey = 'parameterClassifier')
    String classifier

    @Override
    Node handleRequest(Context context) {
        MessageBundle bundle = context.app.bundle
        Node response = new Node(null, 'get')
        Node coordinatesNode = new Node(response, NODE_COORDINATES)
        coordinates.each { coordinate ->
            Node coordinateNode = new Node(coordinatesNode, NODE_COORDINATE, [name: coordinate])
            log.info bundle.getString('log.commandRequest', 'get', coordinate, latest)

            ArtifactCoordinate ac = ArtifactCoordinate.parseCoordinates(coordinate)

            log.debug bundle.getString('log.initialParsedCoordinates', ac)

            if (extension) {
                ac = ac.copyWith(extension: extension)
                if (! ArtifactExtension.values().contains(extension)) {
                    log.warn bundle.getString('log.possibleInvalidExtension', extension)
                }
            }

            if (classifier) {
                ac = ac.copyWith(classifier: classifier)
                if (! ArtifactClassifier.values().contains(classifier)) {
                    log.warn bundle.getString('log.possibleInvalidClassifier', classifier)
                }
            }

            if (latest) {
                String latestVersion = resolver.getArtifactLatestVersion(ac.artifact)
                ac = ac.copyWith(version: latestVersion)
                log.debug bundle.getString('log.determinedLatestVersion', coordinate,latestVersion,ac)
            }

            if (!ac.version && !latest) {
                new Node(coordinateNode, NODE_ERROR, [message: bundle.getString('error.noVersion', ac)])
                return
            }

            ArtifactResult result

            try {
                result = context.resolver.getArtifact(ac.artifact)
            } catch (ArtifactResolutionException are) {
                new Node(coordinateNode, NODE_ERROR, [message: bundle.getString('error.resolutionException', ac, are.message)])
                return
            }

            if (result?.resolved) {
                new Node (coordinateNode, NODE_RESULT, [
                    repository: result.repository.toString(),
                    file: result.artifact.file.absoluteFile.toString()])
            } else {
                new Node(coordinateNode, NODE_ERROR, [message: bundle.getString('error.couldNotResolveArtifact', ac)])
                return
            }
        }
        return response
    }
}
