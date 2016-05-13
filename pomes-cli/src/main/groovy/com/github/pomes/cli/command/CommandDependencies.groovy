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
import com.github.pomes.core.dependency.graph.display.CommandLineDumper
import groovy.util.logging.Slf4j
import org.eclipse.aether.collection.CollectResult

import static org.eclipse.aether.util.artifact.JavaScopes.COMPILE

@Slf4j
@Parameters(commandNames = ['dependencies'], commandDescription = "Displays dependency information for an artifact")
class CommandDependencies implements Command {
    @Parameter(description = '<coordinates>')
    List<String> coordinates

    @Parameter(names = ['-l', '--latest'], description = 'Use the latest version')
    Boolean latest

    @Parameter(names = ['-s', '--scope'], description = 'Sets the dependency scope')
    String scope = COMPILE

    //@Parameter(names = ['-t', '--transitive'], description = 'Resolves transitive dependencies')
    //Boolean transitive = false

    @Override
    void handleRequest(Searcher searcher, Resolver resolver) {
        coordinates.each { coordinate ->
            log.debug "Dependencies request for $coordinate (latest requested: $latest)"
            ArtifactCoordinate ac = ArtifactCoordinate.parseCoordinates(coordinate)

            if (latest) {
                ac = ac.copyWith(version: resolver.getArtifactLatestVersion(ac))
            }

            CollectResult collectResult = resolver.collectAllDependencies(ac.artifact, scope)
            log.debug "Dependency root: ${collectResult.root.artifact}"

            println "Dependencies for $ac (scope: $scope)"
            collectResult.root.accept(new CommandLineDumper())
        }
    }
}
