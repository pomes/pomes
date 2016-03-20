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

package com.github.pomes.core

import com.github.pomes.core.repositories.DefaultLocalRepository
import com.github.pomes.core.repositories.JCenter
import groovy.util.logging.Slf4j
import org.apache.maven.model.Model
import org.apache.maven.model.building.DefaultModelBuilderFactory
import org.apache.maven.model.building.DefaultModelBuildingRequest
import org.apache.maven.model.building.ModelBuilder
import org.apache.maven.model.building.ModelBuildingRequest
import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.graph.DependencyNode
import org.eclipse.aether.impl.DefaultServiceLocator
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.resolution.*
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator
import org.eclipse.aether.version.Version

/**
 *
 *
 * @see <a href="https://www.eclipse.org/aether/documentation/"></a>
 * @see <a href="http://git.eclipse.org/c/aether/aether-demo.git/tree/aether-demo-snippets/src/main/java/"></a>
 * @see <a href="http://download.eclipse.org/aether/aether-core/1.0.1/apidocs/"></a>
 * @see <a href="http://maven.apache.org/ref/3.3.3/apidocs/index.html"></a>
 */
@Slf4j
class Resolver {
    private final LocalRepository localRepository
    private final List<RemoteRepository> remoteRepositories
    private final RepositorySystem repositorySystem
    private final RepositorySystemSession repositorySession


    Resolver(List<RemoteRepository> remoteRepositories = [JCenter.newJCenterRemoteRepository()],
             LocalRepository localRepository = DefaultLocalRepository.newLocalRepository()) {

        this.localRepository = localRepository
        this.remoteRepositories = remoteRepositories.asImmutable()

        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator()
        locator.with {
            addService RepositoryConnectorFactory, BasicRepositoryConnectorFactory
            addService TransporterFactory, FileTransporterFactory
            addService TransporterFactory, HttpTransporterFactory
        }
        repositorySystem = locator.getService(RepositorySystem)

        repositorySession = MavenRepositorySystemUtils.newSession()
        repositorySession.localRepositoryManager =
                repositorySystem.newLocalRepositoryManager(repositorySession, localRepository)
    }

    VersionRangeResult getVersionRangeResult(Artifact artifact, String version = '[0,)', String classifier = ArtifactClassifier.POM) {
        VersionRangeRequest rangeRequest = new VersionRangeRequest()
        rangeRequest.artifact = new DefaultArtifact(artifact.groupId, artifact.artifactId, classifier, version)
        rangeRequest.repositories = remoteRepositories
        return repositorySystem.resolveVersionRange(repositorySession, rangeRequest)
    }

    List<Version> getArtifactVersions(Artifact artifact, String version = '[0,)') {
        return getVersionRangeResult(artifact, version).versions
    }

    List getArtifactLatestVersion(Artifact artifact) {
        return getVersionRangeResult(artifact).highestVersion
    }

/**
 * @see <a href="https://wiki.eclipse.org/Aether/Transitive_Dependency_Resolution">Aether wiki</a>
 */
    DependencyNode getDependencyNode(Artifact artifact, String scope = 'compile') {
        Dependency dependency = new Dependency(artifact, scope)

        CollectRequest collectRequest = new CollectRequest()
        collectRequest.root = dependency
        collectRequest.repositories = remoteRepositories

        DependencyNode node = repositorySystem.collectDependencies(repositorySession, collectRequest).root

        DependencyRequest dependencyRequest = new DependencyRequest()
        dependencyRequest.root = node

        repositorySystem.resolveDependencies(repositorySession, dependencyRequest)

        PreorderNodeListGenerator nlg = new PreorderNodeListGenerator()
        node.accept(nlg)
        return node
    }

    Model getEffectiveModel(Artifact artifact,
                            Boolean processPlugins = false,
                            Boolean twoPhaseBuilding = false,
                            int validationLevel = ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL,
                            Properties systemProperties = System.properties,
                            ModelBuilder modelBuilder = new DefaultModelBuilderFactory().newInstance()) {

        ModelBuildingRequest request = new DefaultModelBuildingRequest()

        request.pomFile = artifact.file
        request.processPlugins = processPlugins
        request.twoPhaseBuilding = twoPhaseBuilding
        request.validationLevel = validationLevel
        request.systemProperties = systemProperties
        request.modelResolver = new ModelResolverImpl(repositorySystem, repositorySession,
                localRepository, remoteRepositories)

        modelBuilder.build(request).effectiveModel
    }

    ArtifactResult getArtifact(String groupId, String artifactId, String version, String classifier = ArtifactClassifier.POM) {
        getArtifact(new DefaultArtifact(groupId, artifactId, classifier, version))
    }

    ArtifactResult getArtifact(Artifact artifact) {
        ArtifactRequest request = new ArtifactRequest(artifact, remoteRepositories, '')
        repositorySystem.resolveArtifact(repositorySession, request)
    }

}
