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
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.version.Version

@Slf4j
@Parameters(commandNames = ['query'], resourceBundle = 'com.github.pomes.cli.MessageBundle', commandDescriptionKey = 'commandDescriptionQuery')
class CommandQuery implements Command {
    MessageBundle bundle = new MessageBundle(ResourceBundle.getBundle('com.github.pomes.cli.MessageBundle'))

    @Parameter(descriptionKey = 'parameterCoordinates')
    List<String> coordinates

    @Parameter(names = ['-l', '--latest'], descriptionKey = 'parameterLatest')
    Boolean latest

    @Override
    Node handleRequest(Context context) {
        Node response = new Node(null, 'query')
        Node coordinatesNode = new Node(response, 'coordinates')
        Resolver resolver = context.resolver
        coordinates.each { coordinate ->
            Node coordinateNode = new Node(coordinatesNode, 'coordinate', [name: coordinate])
            log.info bundle.getString('log.commandRequest', 'query', coordinate, latest)

            ArtifactCoordinate ac = ArtifactCoordinate.parseCoordinates(coordinate)

            if (latest) {
                ac = ac.copyWith(version: resolver.getArtifactLatestVersion(ac))
            }

            if (ac.version) {
                List<Artifact> artifacts = resolver.getClassifiersAndExtensions(ac)
                coordinateNode.append new NodeBuilder().results(count: artifacts.size()) {
                    artifacts.each { a ->
                        artifact name: "$a",
                                classifier: a.classifier,
                                extension: a.extension
                    }
                }
            } else {
                ac = ac.copyWith(version: ArtifactCoordinate.VERSION_OPEN)
                List<Version> versions = resolver.getArtifactVersions(ac)
                String latestVersion = resolver.getArtifactLatestVersion(ac)
                coordinateNode.append new NodeBuilder().results(count: versions.size(),
                        latest: latestVersion) {
                    versions.each { v ->
                        version(name: v, latest: ("$v" == latestVersion))
                    }
                }
            }
        }
        return response
    }
}
