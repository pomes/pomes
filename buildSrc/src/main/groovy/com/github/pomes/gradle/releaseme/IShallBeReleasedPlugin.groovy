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

import groovy.util.logging.Slf4j
import org.ajoberstar.grgit.Status
import org.ajoberstar.grgit.Tag
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.kohsuke.github.GHRepository

import java.time.Year
import java.time.ZoneId

@Slf4j
class IShallBeReleasedPlugin implements Plugin<Project> {
    private static final String CURRENT_VERSION_TASK_NAME = 'displayCurrentVersion'
    private static final String GENERATE_PROJECT_INFO_TASK_NAME = 'generateProjectInfo'
    private static final String CHECK_RELEASE_STATUS_TASK_NAME = 'displayReleaseStatus'
    private static final String PERFORM_RELEASE_TASK_NAME = 'performRelease'

    private static final String DEFAULT_RELEASE_PREFIX = 'version'

    private static final String EXTENSION_NAME = 'releaseme'

    void apply(Project project) {
        def extension = project.extensions.create(EXTENSION_NAME, IShallBeReleasedExtension, project)

        addcheckReleaseStatusTask(project, extension)
        addCurrentVersionTask(project, extension)
        addGenerateProjectInfoTask(project, extension)
    }

    private void addcheckReleaseStatusTask(Project project, IShallBeReleasedExtension extension) {
        project.tasks.create(CHECK_RELEASE_STATUS_TASK_NAME) {
            group = 'release'
            description = 'Checks if there are any local changes.'

            doLast {
                Status status = extension.localGit.status()
                Boolean flag = false
                if (!status.clean) {
                    println "The local git repository contains changes: Conflicts: ${status.conflicts.size()}; Staged: ${status.staged.allChanges.size()}; Unstaged: ${status.unstaged.allChanges.size()}"
                    flag = true
                }

                if (extension.localGit.branch.current.name != extension.ghRepo.defaultBranch) {
                    println "You don't currently appear to be on the default branch (${extension.ghRepo.defaultBranch}) - time to merge (${extension.localGit.branch.current.fullName})."
                    flag = true
                }

                if (!flag) {
                    println 'No issues detected - time to release!'
                }
            }
        }
    }

    private void performRelease(Project project, IShallBeReleasedExtension extension) {
        project.tasks.create(PERFORM_RELEASE_TASK_NAME) {
            group = 'release'
            description = 'Performs the release.'

            doLast {

            }
        }
    }

    private void addCurrentVersionTask(Project project, IShallBeReleasedExtension extension) {
        project.tasks.create(CURRENT_VERSION_TASK_NAME) {
            group = 'release'
            description = 'Displays the current version.'
            doLast {
                println "You are working on ${determineCurrentVersion(extension)} and the next release is ${determineNextReleaseVersion(extension)}"
            }
        }
    }

    private void addGenerateProjectInfoTask(Project project, IShallBeReleasedExtension extension) {
        project.tasks.create(GENERATE_PROJECT_INFO_TASK_NAME) {
            group = 'release'
            description = 'Gathers together various project details.'

            doLast {

                print generateProjectInfo(project, extension.ghRepo).toYaml()
            }
        }
    }

    static String determineCurrentVersion(IShallBeReleasedExtension extension) {
        String currentVersion = '1'
        Boolean snapshot = true
        List<Tag> tags = extension.localGit.tag.list()

        if (tags) {
            Tag latestVersionTag
            List<Tag> versionTags = tags.findAll { it.fullName.startsWith(DEFAULT_RELEASE_PREFIX) }
            latestVersionTag = versionTags.max { it.fullName }
            if (latestVersionTag.commit.id == extension.localGit.head().id) {
                snapshot = false
            }
        }

        snapshot ? "$DEFAULT_RELEASE_PREFIX-$currentVersion-${Snapshot.SNAPSHOT}" : "$DEFAULT_RELEASE_PREFIX-currentVersion"
    }

    static String determineNextReleaseVersion(String currentVersion = determineCurrentVersion()) {
        "$currentVersion".endsWith("$Snapshot.SNAPSHOT") ? "$currentVersion" - "-$Snapshot.SNAPSHOT" : "$DEFAULT_RELEASE_PREFIX-${currentVersion.tokenize('-')[1] + 1}"
    }

    ProjectInfo generateProjectInfo(Project project, GHRepository ghRepo) {
        new ProjectInfo(name: project.name,
                description: ghRepo.description,
                url: ghRepo.homepage.toURL(),
                inceptionYear: new Year(ghRepo.createdAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().year),
                scm: [system               : 'git',
                      url                : ghRepo.gitHttpTransportUrl().toURL(),
                      connection         : "scm:git:${ghRepo.gitHttpTransportUrl()}",
                      developerConnection: "scm:git:${ghRepo.gitHttpTransportUrl()}"],
                licenses: [[name: ghRepo.license.name, url: ghRepo.license.url]],
                issueManagement: [system: 'GitHub', url: "${ghRepo.htmlUrl}/issues".toURL()],
                ciManagement: [system: 'TravisCI', url: "https://travis-ci.org/${ghRepo.fullName}".toURL()]
        )
    }

    /*GHRelease performGithubRelease(String repoName, String tag) {
        GHRepository ghRepo = github.getRepository(repoName)
        GHReleaseBuilder releasePrep = new GHReleaseBuilder(ghRepo, tag)
                .body("TODO: add release notes")
                .draft(true)

        GHRelease release = releasePrep.create()

        //TODO: Upload the archives created by the Application plugin
        // release.uploadAsset()
        release.draft = false
        return release
    }*/
}
