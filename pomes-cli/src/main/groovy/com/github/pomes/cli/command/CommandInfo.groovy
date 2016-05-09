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
import com.github.pomes.core.ArtifactExtension
import com.github.pomes.core.Resolver
import com.github.pomes.core.Searcher
import groovy.text.GStringTemplateEngine
import groovy.util.logging.Slf4j
import org.apache.maven.model.Model
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.graph.Dependency

@Slf4j
@Parameters(commandNames = ['info'], commandDescription = "Gets information about an artifact")
class CommandInfo implements Command {
    @Parameter(description = '<coordinates>')
    List<String> coordinates

    @Parameter(names = ['-l', '--latest'], description = 'Use the latest version')
    Boolean latest

    @Parameter(names = ['-s', '--scope'], description = 'Sets the dependency scope (default is \'compile\')')
    String scope = 'compile'

    @Parameter(names = ['-d', '--dependencies'], description = 'Resolves dependencies')
    Boolean dependencies = false

    @Parameter(names = ['-t', '--transitive'], description = 'Resolves transitive dependencies')
    Boolean transitive = false

    @Override
    void handleRequest(Searcher searcher, Resolver resolver) {
        coordinates.each { coordinate ->
            log.debug "Info request for $coordinate (latest requested: $latest)"
            ArtifactCoordinate ac = ArtifactCoordinate.parseCoordinates(coordinate)

            if (latest) {
                ac = ac.copyWith(version: resolver.getArtifactLatestVersion(ac))
            }

            if (!ac.version) {
                System.err.println "GAV is required - please provide a version for $ac"
                log.error "info command failed as coordinate ($ac) doesn't provide a version and --latest not requested"
                System.exit(-1)
            }

            if (ac.extension != ArtifactExtension.POM.value)
                ac = ac.copyWith(extension: ArtifactExtension.POM.value)

            Artifact artifact = resolver.getArtifact(ac).artifact
            Model model = resolver.getEffectiveModel(artifact)


            List<Dependency> dependencyList = resolver.getAllDependencies(artifact, scope)

                /*
                DependencyNode dependencyNode

            StringBuilder dependencyTree
                if (dependencies || transitive) {
                dependencyNode = resolver.getDependencyNode(artifact, scope)
                dependencyTree = new StringBuilder()

                def nodeWalker
                nodeWalker = { DependencyNode node, Integer pad = 1 ->
                    log.debug("Children for $node.artifact: ${node.children*.artifact.toString()}")
                    for (DependencyNode child in node.children) {
                        dependencyTree << "${' ' * pad}- $child.artifact (version constraint: ${child.versionConstraint})\n"
                        if (transitive) {
                            //TODO: This will cause a stack overflow because of cycles in the dependency tree
                            //      I need to implement a visitor class that handles this!
                            //      Perhaps: RepositorySystemSession.getDependencyTraverser
                            //output << nodeWalkerTrampoline(resolver.getDependencyNode(child.artifact, scope), pad + 2)
                        }
                    }
                }

                nodeWalker(dependencyNode)
                }
                */


            URL template = this.class.getResource('/com/github/pomes/cli/templates/model/details.txt')

            if (template) {
                GStringTemplateEngine engine = new GStringTemplateEngine()

                println engine.createTemplate(template)
                        .make([model       : model,
                               dependencies: dependencyList])
                        .toString()
            } else {
                System.err.println "Failed to load the requested template"
                System.exit(-1)
            }
        }
    }
}
