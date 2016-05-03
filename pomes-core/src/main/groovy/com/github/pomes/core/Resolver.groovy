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
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

import java.util.regex.Pattern

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
    /**
     * This is most likely pointing to a local directory
     */
    final LocalRepository localRepository

    /**
     * A list of remote Maven repositories
     */
    final List<RemoteRepository> remoteRepositories

    /**
     *
     */
    final RepositorySystem repositorySystem

    /**
     *
     */
    final RepositorySystemSession repositorySession

    /**
     * Prepares the instance by preparing a RepositorySystem instance and a RepositorySystemSession
     * @param remoteRepositories the list of remote repos. Default is a single item {@link com.github.pomes.core.repositories.JCenter}
     * @param localRepository is the local repo location. Default is {@link com.github.pomes.core.repositories.DefaultLocalRepository}
     */
    Resolver(List<RemoteRepository> remoteRepositories = [JCenter.newJCenterRemoteRepository()],
             LocalRepository localRepository = DefaultLocalRepository.repository) {

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

    /**
     * Used to determine version range for an artifact
     *
     * @param artifact The artifact for which we wish to determine possible versions
     * @param version the version specifier
     * @param extension the desired file extension (e.g. 'pom')
     * @return the matching versions
     *
     * @see <a href="https://maven.apache.org/pom.html#Dependency_Version_Requirement_Specification">Version requirement
     *  specification</a>
     */
    VersionRangeResult getVersionRangeResult(Artifact artifact, String version = '[0,)') {
        VersionRangeRequest rangeRequest = new VersionRangeRequest()
        rangeRequest.artifact = new DefaultArtifact(artifact.groupId, artifact.artifactId, artifact.classifier, artifact.extension, version)
        rangeRequest.repositories = remoteRepositories
        return repositorySystem.resolveVersionRange(repositorySession, rangeRequest)
    }

    VersionRangeResult getVersionRangeResult(ArtifactCoordinate coordinate, String version = '[0,)') {
        getVersionRangeResult(coordinate.artifact, version)
    }

    /**
     * Helper function for {@link #getVersionRangeResult}
     * @param artifact
     * @param version
     * @return
     */
    List<Version> getArtifactVersions(Artifact artifact, String version = '[0,)') {
        return getVersionRangeResult(artifact, version).versions
    }

    List<Version> getArtifactVersions(ArtifactCoordinate coordinate, String version = '[0,)') {
        return getVersionRangeResult(coordinate.artifact, version).versions
    }

    /**
     *
     * @param groupId
     * @param artifactId
     * @param version
     * @return
     */
    List<Version> getArtifactVersions(String groupId, String artifactId, String version = '[0,)') {
        return getVersionRangeResult(new DefaultArtifact("$groupId:$artifactId"), version).versions
    }

    /**
     * Helper function for {@link #getVersionRangeResult} that returns just the latest version
     * @param artifact
     * @return
     */
    String getArtifactLatestVersion(Artifact artifact) {
        log.debug "Determing latest version of $artifact"
        return getVersionRangeResult(artifact).highestVersion.toString()
    }

    /**
     * Determines the latest version of an artifact.
     *
     * Handles a GA coordinate by using #ArtifactCoordinate.VERSION_OPEN as the version
     *
     * @param coordinate
     * @return
     */
    String getArtifactLatestVersion(ArtifactCoordinate coordinate) {
        if (!coordinate.version)
            getArtifactLatestVersion(coordinate.copyWith(version: ArtifactCoordinate.VERSION_OPEN))
        else
            getArtifactLatestVersion(coordinate.artifact)
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

    /**
     * Largely wraps around {@link com.github.pomes.core.ModelResolverImpl} to determine
     * the effective POM
     * @param artifact
     * @param processPlugins
     * @param twoPhaseBuilding
     * @param validationLevel
     * @param systemProperties
     * @param modelBuilder
     * @return
     */
    Model getEffectiveModel(Artifact artifact,
                            Boolean processPlugins = false,
                            Boolean twoPhaseBuilding = false,
                            int validationLevel = ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL,
                            Properties systemProperties = System.properties,
                            ModelBuilder modelBuilder = new DefaultModelBuilderFactory().newInstance()) {
        log.debug "Preparing effective model for $artifact ($artifact.file)"

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

    /**
     *
     * @param groupId
     * @param artifactId
     * @param version
     * @param packaging
     * @return
     */
    ArtifactResult getArtifact(String groupId, String artifactId, String version, String extension = null, String classifier = '') {
        getArtifact(new DefaultArtifact(groupId, artifactId, classifier, extension, version))
    }

    ArtifactResult getArtifact(ArtifactCoordinate coordinate) {
        getArtifact(coordinate.artifact)
    }

    /**
     *
     * @param artifact
     * @return
     */
    ArtifactResult getArtifact(Artifact artifact) throws ArtifactResolutionException {
        log.debug("Getting artifact: $artifact")
        ArtifactRequest request = new ArtifactRequest(artifact, remoteRepositories, '')
        ArtifactResult result = repositorySystem.resolveArtifact(repositorySession, request)
        log.debug("Artifact $artifact was resolved (got)")
        return result
    }

    List<Artifact> getClassifiersAndExtensions(ArtifactCoordinate coordinate) {
        getClassifiersAndExtensions(coordinate.artifact)
    }

    /**
     * Attempts to determine the classifiers and extensions for a GA
     *
     * TODO: Needs a lot more testing
     * Assumes: A lot! Assumes that a remote repo is used and that
     * the directory listing can be parsed
     *
     * @param artifact
     */
    List<Artifact> getClassifiersAndExtensions(Artifact artifact) {
        List<Artifact> results = []
        Pattern hrefPattern = ~/$artifact.artifactId-$artifact.version[-]?([^\. ]*).(.*)/

        ArtifactResult result = getArtifact(artifact)
        String artifactBaseUrl = "${result.repository.url}${artifact.groupId.replace('.', '/')}/${artifact.artifactId}/${artifact.version}"
        log.debug "Attempting to read directory listing from: $artifactBaseUrl"
        Elements links = Jsoup.parse(artifactBaseUrl.toURL(), 30_000).select("a[href]")
        links.each { Element link ->
            String href = link.ownText()
            def matcher = hrefPattern.matcher(href)
            if (matcher.matches()) {
                results << new DefaultArtifact(artifact.groupId,
                        artifact.artifactId,
                        matcher.group(1),
                        matcher.group(2),
                        artifact.version)
            } else {
                log.debug "Couldn't extract the classifier & extension for $href"
            }
        }
        return results
    }
}
