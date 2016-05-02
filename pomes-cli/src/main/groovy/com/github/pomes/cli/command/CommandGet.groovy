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
import com.github.pomes.core.ArtifactClassifier
import com.github.pomes.core.ArtifactCoordinate
import com.github.pomes.core.ArtifactExtension
import com.github.pomes.core.Resolver
import com.github.pomes.core.Searcher
import groovy.util.logging.Slf4j
import org.eclipse.aether.resolution.ArtifactResolutionException
import org.eclipse.aether.resolution.ArtifactResult

@Slf4j
@Parameters(commandNames = ['get'], commandDescription = "Gets (installs) an artifact. Default request is for POM file.")
class CommandGet implements Command {
    @Parameter(description = '<coordinates>')
    List<String> coordinates

    @Parameter(names = ['-l', '--latest'], description = 'Use the latest version')
    Boolean latest = false

    @Parameter(names = ['-e', '--extension'], description = 'Request a specific packaging type (e.g. pom or jar)')
    String extension

    @Parameter(names = ['-c', '--classifier'], description = 'Request a specific classifier (e.g. javadoc or sources)')
    String classifier

    @Override
    void handleRequest(Searcher searcher, Resolver resolver) {
        coordinates.each { coordinate ->
            log.debug "Get request for $coordinate (latest requested: $latest)"

            ArtifactCoordinate ac = ArtifactCoordinate.parseCoordinates(coordinate)

            log.debug "Initial parsed coordinates: $ac"

            if (extension) {
                ac = ac.copyWith(extension: extension)
                if (! ArtifactExtension.values().contains(extension)) {
                    log.warn "The extension ($extension) may not be valid "
                }
            }

            if (classifier) {
                ac = ac.copyWith(classifier: classifier)
                if (! ArtifactClassifier.values().contains(classifier)) {
                    log.warn "The classifier ($classifier) may not be valid "
                }
            }

            if (latest) {
                String latestVersion = resolver.getArtifactLatestVersion(ac.artifact)
                ac = ac.copyWith(version: latestVersion)
                log.debug("Determined latest version for $coordinate is $latestVersion. Using $ac")
            }

            if (!ac.version && !latest) {
                System.err.println "Coordinate requires a version (or request latest)"
                System.exit(-1)
            }

            ArtifactResult result

            try {
                result = resolver.getArtifact(ac.artifact)
            } catch (ArtifactResolutionException are) {
                System.err.println "Received a resolution exception for ${ac.artifact}: ${are.message}"
                System.exit(-1)
            }
            if (result?.resolved) {
                println "Coordinates: $result.artifact"
                println "Repository: ${result.repository.toString()}"
                println "Local copy: ${result.artifact.file.absoluteFile.toString()}"
            } else {
                System.err.println "Could not resolve ${ac.artifact}"
                System.exit(-1)
            }
        }
    }
}
