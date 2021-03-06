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

package com.github.pomes.core.repositories

import groovy.util.logging.Slf4j
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository

import java.nio.file.Path
import java.nio.file.Paths

/**
 * An easy option for a LocalRepository instance: ~/.pomes/repository.
 *
 * Limitation: As this uses the user's home dir as a base this isn't useful for implementations
 * that may have multiple users needing their own local repo. You'll need to create your own
 * LocalRepository.
 */
@Slf4j
final class DefaultLocalRepository {
    /**
     * The Path to the default local repo.
     */
    static final Path baseDir

    /**
     * I'm not really sure about Aether's use of this.
     */
    static final String repositoryType = null

    /**
     * An instance of the repository that could be used instead of creating a new instance
     * with {@link #newLocalRepository()}.
     */
    static final LocalRepository repository

    static {
        /*
         * I've used ~/.pomes/repository instead of ~/.m2/repository so there's no cross-over
         * with the default Maven setup.
         */
        baseDir = Paths.get(System.getProperty('user.home'), '.pomes', 'repository')
        repository = new LocalRepository(baseDir.toFile(), repositoryType)
    }

    /**
     * Use instead of {@link #repository} if you need a new instance
     * @return a LocalRepository instance using a default local repo definition
     */
    static LocalRepository newLocalRepository() {
        new LocalRepository(baseDir, repositoryType)
    }
}
