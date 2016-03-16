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
 * Implementation of the ModelResolver interface
 */
public class ModelResolverImpl
        implements ModelResolver {

    private List<RemoteRepository> remoteRepositories
    private final LocalRepository localRepo
    private final RepositorySystem repoSystem
    private final RepositorySystemSession session

    ModelResolverImpl(
            RepositorySystem repoSystem,
            RepositorySystemSession session,
            LocalRepository localRepo,
            List<RemoteRepository> remoteRepositories) {
        this.remoteRepositories = remoteRepositories
        this.localRepo = localRepo
        this.repoSystem = repoSystem
        this.session = session
    }

    @Override
    ModelSource2 resolveModel(String groupId, String artifactId, String version) throws UnresolvableModelException {
        Artifact artifact = new DefaultArtifact(groupId, artifactId, ArtifactClassifier.POM, version)

        ArtifactRequest request = new ArtifactRequest(artifact, remoteRepositories, '')
        ArtifactResult result = repoSystem.resolveArtifact(session, request)

        return new FileModelSource(result.artifact.file)
    }

    @Override
    ModelSource2 resolveModel(Parent parent) throws UnresolvableModelException {
        Artifact artifact = new DefaultArtifact(parent.groupId, parent.artifactId, ArtifactClassifier.POM, parent.version)

        ArtifactRequest request = new ArtifactRequest(artifact, remoteRepositories, '')
        ArtifactResult result = repoSystem.resolveArtifact(session, request)

        return new FileModelSource(result.artifact.file)
    }

    /**
     *
     * @param repository
     * @throws InvalidRepositoryException
     */
    void addRepository(Repository repository, boolean replace = true) throws InvalidRepositoryException {
        if (!replace && remoteRepositories.find{it.id == repository.id}) {
            return
        }

        remoteRepositories."$repository.id" = repository
    }

    @Override
    ModelResolver newCopy() {
        return new ModelResolverImpl(repoSystem,session,localRepo,remoteRepositories)
    }
}
