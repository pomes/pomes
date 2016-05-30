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

import groovy.util.logging.Slf4j
import org.apache.maven.model.Parent
import org.apache.maven.model.Repository
import org.apache.maven.model.building.FileModelSource
import org.apache.maven.model.building.ModelSource2
import org.apache.maven.model.resolution.InvalidRepositoryException
import org.apache.maven.model.resolution.ModelResolver
import org.apache.maven.model.resolution.UnresolvableModelException
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.resolution.ArtifactRequest
import org.eclipse.aether.resolution.ArtifactResult

/**
 * Implementation of the {@link org.apache.maven.model.resolution.ModelResolver} interface
 */
@Slf4j
public class ModelResolverImpl
        implements ModelResolver {

    private List<Repository> remoteRepositories = []
    private final LocalRepository localRepository
    private final RepositorySystem repositorySystem
    private final RepositorySystemSession repositorySession

    ModelResolverImpl(
            RepositorySystem repositorySystem,
            RepositorySystemSession repositorySession,
            LocalRepository localRepository,
            List<RemoteRepository> remoteRepositories) {
        this.localRepository = localRepository
        this.repositorySystem = repositorySystem
        this.repositorySession = repositorySession
        remoteRepositories.each { repo ->
            addRepository(repo)
        }
    }

    @Override
    ModelSource2 resolveModel(String groupId, String artifactId, String version) throws UnresolvableModelException {
        Artifact artifact = new DefaultArtifact(groupId, artifactId, ArtifactClassifier.POM.value, version)

        log.debug "Resolving model for $artifact"

        ArtifactRequest request = new ArtifactRequest(artifact, remoteRepositories, '')
        ArtifactResult result = repositorySystem.resolveArtifact(repositorySession, request)

        return new FileModelSource(result.artifact.file)
    }

    @Override
    ModelSource2 resolveModel(Parent parent) throws UnresolvableModelException {
        Artifact artifact = new DefaultArtifact(parent.groupId, parent.artifactId, ArtifactExtension.POM.value, parent.version)

        log.debug "Resolving model for parent $artifact"

        ArtifactResult result = repositorySystem.resolveArtifact(repositorySession,
                new ArtifactRequest(artifact, remoteRepositories, null))

        return new FileModelSource(result.artifact.file)
    }

    void addRepository(RemoteRepository repository, boolean replace = true) throws InvalidRepositoryException {
        Repository repo = new Repository()
        repo.with {
            id = repository.id
            url = repository.url
        }
        addRepository(repo)
    }

    @Override
    void addRepository(Repository repository, boolean replace = true) throws InvalidRepositoryException {
        log.debug "Adding repository ${repository} (replace = $replace)"

        RemoteRepository existingRepo = remoteRepositories.find{it.id == repository.id}

        if (existingRepo) {
            if (replace) {
                remoteRepositories.remove(existingRepo)
                remoteRepositories << repository
                return
            } else {
                //no-op as we won't replace
                return
            }
        }

        remoteRepositories << repository
    }

    @Override
    ModelResolver newCopy() {
        return new ModelResolverImpl(repositorySystem,repositorySession,localRepository,remoteRepositories)
    }
}
