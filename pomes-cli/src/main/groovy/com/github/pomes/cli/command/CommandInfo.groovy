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
import groovy.util.logging.Slf4j
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.version.Version

@Slf4j
@Parameters(commandNames = ['info'], commandDescription = "Gets information about an artifact")
class CommandInfo implements Command {
    @Parameter(description = '<coordinates>')
    List<String> coordinates

    @Parameter(names = ['-l', '--latest'], description = 'Use the latest version')
    Boolean latest

    @Override
    void handleRequest(Searcher searcher, Resolver resolver) {
        coordinates.each { coordinate ->
            log.debug "Info request for $coordinate (latest requested: $latest)"
            ArtifactCoordinate ac = ArtifactCoordinate.parseCoordinates(coordinate)

            //TODO: Handle --latest

            if (ac.version) {
                List<Artifact> artifacts = resolver.getClassifiersAndExtensions(ac)

                println "${artifacts.size()} available classifiers and extensions for $ac"
                artifacts.each { artifact ->
                    println " - $artifact - classifier:'${artifact.classifier}' extension:'${artifact.extension}'"
                }
            } else {
                List<Version> versions = resolver.getArtifactVersions(ac.artifact)
                println "${versions.size()} available versions for $ac"
                versions.each { version ->
                    println " - $version"
                }
            }
        }
    }
}
