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
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Status
import org.ajoberstar.grgit.Tag
import org.apache.commons.validator.routines.UrlValidator
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub
import org.kohsuke.github.HttpConnector
import org.kohsuke.github.extras.PreviewHttpConnector

import java.nio.file.Files
import java.nio.file.Paths

@Slf4j
class IShallBeReleasedPlugin implements Plugin<Project> {
    static final String EXTENSION_NAME = 'releaseme'

    //Tasks:
    static final String DETERMINE_VERSION_TASK_NAME = 'determineCurrentVersion'
    static final String DISPLAY_VERSION_TASK_NAME = 'displayCurrentVersion'
    static final String CHECK_RELEASE_STATUS_TASK_NAME = 'checkReleaseStatus'
    static final String PERFORM_RELEASE_TASK_NAME = 'performRelease'
    static final String CONFIGURE_VERSION_FILE_TASK_NAME = 'configureVersionFile'

    static final String DEFAULT_RELEASE_TAG_PREFIX = 'version'

    Grgit localGit

    GHRepository ghRepo

    String ghConnection, ghProject

    HttpConnector ghConnector = new PreviewHttpConnector()

    IShallBeReleasedExtension extension

    @Override
    void apply(Project project) {
        extension = project.extensions.create(EXTENSION_NAME, IShallBeReleasedExtension)

        try {
            localGit = Grgit.open(currentDir: "${project.rootDir}")
        } catch (RepositoryNotFoundException e) {
            throw new GradleException("Git repository not found at ${project.rootDir}")
        }
        setVersion(project)

        ghConnection = localGit.remote.list().find { it.name == extension.remote }?.url
        log.debug "Remote GitHub connection: $ghConnection"

        if (ghConnection.startsWith('git@github.com')) {
            ghProject = ghConnection.tokenize(':')[1] - '.git'
        } else {
            UrlValidator urlValidator = new UrlValidator()
            if (urlValidator.isValid(ghConnection)) {
                ghProject = (ghConnection.toURL().path - '.git').substring(1)
            } else {
                throw new GradleException("Unable to determine the Github project for $ghConnection")
            }
        }
        log.debug "GitHub project: $ghProject"

        try {
            extension.gitHub = GitHub.connect()
        } catch (IOException ex) {
            throw new GradleException('Failed when trying to connect to GitHub')
        }
        extension.gitHub.connector = ghConnector

        try {
            ghRepo = extension.gitHub.getRepository(ghProject)
        } catch (IOException ex) {
            throw new GradleException("Failed when trying to connect to GitHub project ($ghProject)")
        }

        project.ext.ghRepo = ghRepo
        configureTasks(project, extension)
    }

    private void configureTasks(final Project project, final IShallBeReleasedExtension extension) {
        addDetermineVersionTask(project, extension)
        addDisplayVersionTask(project, extension)
        addConfigureVersionFileTask(project, extension)
        addCheckReleaseStatusTask(project, extension)
        addPerformReleaseTask(project, extension)
    }

    private void addCheckReleaseStatusTask(Project project, IShallBeReleasedExtension extension) {
        project.tasks.create(CHECK_RELEASE_STATUS_TASK_NAME) {
            group = 'release'
            description = 'Checks if there are any items preventing a release.'
            doLast {
                Status status = localGit.status()
                Boolean flag = false
                List<String> errors = []
                if (!status.clean) {
                    errors << "The local git repository contains changes: Conflicts: ${status.conflicts.size()}; Staged: ${status.staged.allChanges.size()}; Unstaged: ${status.unstaged.allChanges.size()}"
                    flag = true
                }

                if (localGit.branch.current.name != ghRepo.defaultBranch) {
                    errors << "You don't currently appear to be on the default branch (${ghRepo.defaultBranch}) - time to merge (${localGit.branch.current.fullName})."
                    flag = true
                }

                if (Files.notExists(Paths.get(project.rootDir.toString(), 'LICENSE'))) {
                    errors << 'You don\'t have a LICENSE file'
                    flag = true
                }

                if (!flag) {
                    log.info 'No issues detected - time to release!'
                } else {
                    errors.each {
                        log.error it
                    }
                    throw new GradleException('Issues detected - cannot perform a release!')
                }
            }
        }
    }

    private void addPerformReleaseTask(Project project, IShallBeReleasedExtension extension) {

        project.tasks.create('_prepareReleaseVersion') {
            doLast {
                //Change the version to drop the SNAPSHOT
                project.version = determineNextReleaseVersion(project.version)
                project.subprojects.each { sub ->
                    sub.version = project.version
                }
                println "Set the project version to $project.version"
            }
        }

        project.tasks.create(PERFORM_RELEASE_TASK_NAME) {
            group = 'release'
            description = 'Performs the release.'
            dependsOn CHECK_RELEASE_STATUS_TASK_NAME
            dependsOn '_prepareReleaseVersion', CONFIGURE_VERSION_FILE_TASK_NAME
            doLast {
                String tag = "${DEFAULT_RELEASE_TAG_PREFIX}-${project.version}"
                println "Releasing $tag"

                //Commit the changes
                //def commit = localGit.commit(message: "Preparing version ${project.version} release", all: true)
                //Tag the repository
                //localGit.tag.add(name: tag, pointsTo: commit)

                //Move the project to the next snapshot version
                //project.version = determineNextReleaseVersion(project.version)
            }
        }
    }

    private void addDetermineVersionTask(Project project, IShallBeReleasedExtension extension) {
        project.tasks.create(DETERMINE_VERSION_TASK_NAME) {
            group = 'release'
            description = 'Determines the current version.'
            onlyIf {
                if (project.ext.has('lastVersion')) {
                    project.ext.lastVersion != determineCurrentVersion(localGit)
                } else {
                    true
                }
            }
            doLast {
                setVersion(project)
            }
            finalizedBy CONFIGURE_VERSION_FILE_TASK_NAME
        }
    }

    void setVersion(Project project) {
        project.version = determineCurrentVersion(localGit)
        project.ext.lastVersion = project.version
        project.subprojects.each { sub ->
            sub.version = project.version
        }
    }

    private void addConfigureVersionFileTask(Project project, IShallBeReleasedExtension extension) {
        File vFile = project.file("${project.rootDir}/VERSION")
        project.tasks.create(CONFIGURE_VERSION_FILE_TASK_NAME) {
            group = 'release'
            description = 'Adds a VERSION file to the project root'
            dependsOn DETERMINE_VERSION_TASK_NAME
            //mustRunAfter DETERMINE_VERSION_TASK_NAME
            inputs.file vFile
            doLast {
                vFile.text = project.version
                log.info "Configured version file: $vFile"
            }
        }
    }

    private void addDisplayVersionTask(Project project, IShallBeReleasedExtension extension) {
        project.tasks.create(DISPLAY_VERSION_TASK_NAME) {
            group = 'release'
            description = 'Displays the current version.'
            dependsOn DETERMINE_VERSION_TASK_NAME
            doLast {
                println project.version
            }
        }
    }

    static String determineCurrentVersion(Grgit localGit) {
        String currentVersion = '1'
        Boolean snapshot = true
        List<Tag> tags = localGit.tag.list()

        if (tags) {
            Tag latestVersionTag
            List<Tag> versionTags = tags.findAll { it.fullName.startsWith(DEFAULT_RELEASE_TAG_PREFIX) }
            latestVersionTag = versionTags.max { it.fullName }
            if (latestVersionTag.commit.id == localGit.head().id) {
                snapshot = false
            }
        }
        snapshot ? "$currentVersion-${Snapshot.SNAPSHOT}" : "$currentVersion"
    }

    static String determineNextReleaseVersion(String currentVersion,
                                              String releaseTagPrefix = DEFAULT_RELEASE_TAG_PREFIX) {
        "$currentVersion".endsWith("$Snapshot.SNAPSHOT") ? "$currentVersion" - "-$Snapshot.SNAPSHOT" : "$releaseTagPrefix-${currentVersion.tokenize('-')[1] + 1}"
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
