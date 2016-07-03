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

package com.github.pomes.gradle.releaseme

import com.github.pomes.gradle.releaseme.project.ProjectInfo
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.ajoberstar.grgit.Grgit
import org.gradle.api.Project
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub
import org.kohsuke.github.HttpConnector
import org.kohsuke.github.extras.PreviewHttpConnector

@Slf4j
@ToString(includeNames = true)
class IShallBeReleasedExtension {
    protected final Project project

    static final String DEFAULT_RELEASE_TAG_PREFIX = 'version'

    String remote = 'origin'

    final Grgit localGit

    final GHRepository ghRepo

    URL ghUrl

    HttpConnector ghConnector = new PreviewHttpConnector()

    String nextReleaseVersion

    ProjectInfo projectInfo

    Boolean releaseProject = false

    String mainClassName

    Boolean githubRelease = false

    Boolean bintrayRelease = false

    String releaseTagPrefix = DEFAULT_RELEASE_TAG_PREFIX

    IShallBeReleasedExtension(Project project) {
        this.project = project

        localGit = Grgit.open(currentDir: "${project.rootDir}")

        ghUrl = localGit.remote.list().find { it.name == remote }?.url.toURL()

        GitHub gh = GitHub.connect()
        gh.connector = ghConnector
        ghRepo = gh.getRepository((ghUrl.path - '.git').substring(1))
    }

    String toString() {
        """\
Project name: ${project.name}
Project version: ${project.version}
Release this project: $releaseProject
Github project: ${ghRepo.fullName}
Github URL: $ghUrl
Release to Github: $githubRelease
Release to Bintray: $bintrayRelease
Main class name: $mainClassName
Release (git) tag prefix: $releaseTagPrefix"""
    }
}
