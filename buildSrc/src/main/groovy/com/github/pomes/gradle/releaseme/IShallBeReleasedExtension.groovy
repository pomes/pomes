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

import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.ajoberstar.grgit.Grgit
import org.gradle.api.Project
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub
import org.kohsuke.github.HttpConnector
import org.kohsuke.github.extras.PreviewHttpConnector

import static com.github.pomes.gradle.releaseme.IShallBeReleasedPlugin.determineCurrentVersion

@Slf4j
@ToString(includeNames = true)
class IShallBeReleasedExtension {
    protected final Project project

    String remote = 'origin'

    Grgit localGit

    GHRepository ghRepo

    HttpConnector ghConnector = new PreviewHttpConnector()

    String version

    IShallBeReleasedExtension(Project project) {
        this.project = project

        localGit = Grgit.open(currentDir: "${project.rootDir}")

        URL ghUrl = localGit.remote.list().find { it.name == 'origin' }?.url.toURL()

        GitHub gh = GitHub.connect()
        gh.connector = ghConnector
        ghRepo = gh.getRepository((ghUrl.path - '.git').substring(1))

        version = determineCurrentVersion(this)
    }
}
