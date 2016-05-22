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
import groovy.util.logging.Slf4j
import org.apache.maven.model.Model
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.graph.Dependency

@Slf4j
@Parameters(commandNames = ['info'], resourceBundle = 'com.github.pomes.cli.MessageBundle', commandDescriptionKey = 'commandDescriptionInfo')
class CommandInfo implements Command {
    MessageBundle bundle = new MessageBundle(ResourceBundle.getBundle('com.github.pomes.cli.MessageBundle'))

    @Parameter(descriptionKey = 'parameterCoordinates')
    List<String> coordinates

    @Parameter(names = ['-l', '--latest'], descriptionKey = 'parameterLatest')
    Boolean latest

    @Parameter(names = ['-s', '--scope'], descriptionKey = 'parameterScope')
    String scope = 'compile'

    @Override
    Node handleRequest(Context context) {
        Node response = new Node(null, 'info')
        Node coordinatesNode = new Node(response, 'coordinates')
        Resolver resolver = context.resolver
        coordinates.each { coordinate ->
            Node coordinateNode = new Node(coordinatesNode, 'coordinate', [name: coordinate])
            log.info bundle.getString('log.commandRequest', 'info', coordinate, latest)

            ArtifactCoordinate ac = ArtifactCoordinate.parseCoordinates(coordinate)

            if (latest) {
                ac = ac.copyWith(version: resolver.getArtifactLatestVersion(ac))
            }

            if (!ac.version) {
                new Node(coordinateNode, 'error', [message: bundle.getString('error.GAVRequired', ac)])
                return
            }

            if (ac.extension != ArtifactExtension.POM.value)
                ac = ac.copyWith(extension: ArtifactExtension.POM.value)

            Artifact artifact = resolver.getArtifact(ac).artifact
            Model effectiveModel = resolver.getEffectiveModel(artifact)

            List<Dependency> dependencyList = resolver.getDirectDependencies(artifact)

            coordinateNode.append new NodeBuilder().artifact {
                model(name: effectiveModel.toString(),
                        artifactId: effectiveModel.artifactId,
                        groupId: effectiveModel.groupId,
                        version: effectiveModel.version,
                        packaging: effectiveModel.packaging,
                        inceptionYear: effectiveModel.inceptionYear,
                        description: effectiveModel.description) {
                    parent(name: effectiveModel.parent?.toString(),
                            artifactId: effectiveModel.parent?.artifactId,
                            groupId: effectiveModel.parent?.groupId,
                            version: effectiveModel.parent?.version)
                    organization(name: effectiveModel.organization?.name,
                            url: effectiveModel.organization?.url)
                    licenses {
                        effectiveModel.licenses.each { lic ->
                            license(name: lic.name,
                                    url: lic.url,
                                    distribution: lic.distribution,
                                    comments: lic.comments)
                        }
                    }
                    scm(connection: effectiveModel.scm?.connection,
                            url: effectiveModel.scm?.url,
                            developerConnection: effectiveModel.scm?.developerConnection
                    )
                    ciManagement(system: effectiveModel.ciManagement?.system,
                            url: effectiveModel.ciManagement?.url)
                    issueManagement(system: effectiveModel.issueManagement?.system,
                            url: effectiveModel.issueManagement?.url)
                    mailingLists {
                        effectiveModel.mailingLists.each { ml ->
                            mailingList(name: ml.name,
                                    archive: ml.archive)
                        }
                    }
                }
                dependencies {
                    dependencyList.sort(false, { it.scope }).each { dep ->
                        dependency(name: dep.artifact.toString(),
                                scope: dep.scope,
                                optional: dep.optional) {
                            exclusions {
                                dep.exclusions.each { ex ->
                                    exclusion(artifactId: ex.artifactId,
                                            groupId: ex.groupId,
                                            classifier: ex.classifier,
                                            extension: ex.extension
                                    )
                                }
                            }
                        }
                    }
                }
            }

            /*
            TODO: delete once template model is in place
            URL template = this.class.getResource('/com/github/pomes/cli/templates/model/details.txt')

            if (template) {
                GStringTemplateEngine engine = new GStringTemplateEngine()

                println engine.createTemplate(template)
                        .make([model       : model,
                               dependencies: dependencyList.sort(false, { it.scope })])
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


