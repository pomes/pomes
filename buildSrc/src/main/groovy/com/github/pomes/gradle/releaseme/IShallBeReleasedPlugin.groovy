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

import com.github.pomes.gradle.releaseme.project.*
import groovy.util.logging.Slf4j
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Status
import org.ajoberstar.grgit.Tag
import org.apache.commons.validator.routines.UrlValidator
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.tasks.bundling.Jar
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub
import org.kohsuke.github.HttpConnector
import org.kohsuke.github.extras.PreviewHttpConnector

import java.nio.file.Files
import java.nio.file.Paths
import java.time.Year
import java.time.ZoneId

import static org.gradle.api.plugins.BasePlugin.BUILD_GROUP
import static org.gradle.api.plugins.GroovyPlugin.GROOVYDOC_TASK_NAME
import static org.gradle.api.plugins.JavaBasePlugin.CHECK_TASK_NAME
import static org.gradle.api.plugins.JavaPlugin.CLASSES_TASK_NAME
import static org.gradle.api.plugins.JavaPlugin.TEST_CLASSES_TASK_NAME

@Slf4j
class IShallBeReleasedPlugin implements Plugin<Project> {
    static final String EXTENSION_NAME = 'releaseme'

    //Tasks:
    static final String DETERMINE_VERSION_TASK_NAME = 'determineCurrentVersion'
    static final String DISPLAY_VERSION_TASK_NAME = 'displayCurrentVersion'
    static final String GENERATE_PROJECT_INFO_TASK_NAME = 'generateProjectInfo'
    static final String DISPLAY_PROJECT_INFO_TASK_NAME = 'displayProjectInfo'
    static final String CHECK_RELEASE_STATUS_TASK_NAME = 'checkReleaseStatus'
    static final String PERFORM_RELEASE_TASK_NAME = 'performRelease'
    static final String CONFIGURE_VERSION_FILE_TASK_NAME = 'configureVersionFile'
    static final String CONFIGURE_POM_TASK_NAME = 'configurePom'
    //static final String RELEASE_ME_PROPERTIES_TASK_NAME = 'releaseMeProperties'

    static final String DEFAULT_RELEASE_TAG_PREFIX = 'version'

    Grgit localGit

    GHRepository ghRepo

    String ghConnection, ghProject

    HttpConnector ghConnector = new PreviewHttpConnector()

    String nextReleaseVersion

    IShallBeReleasedExtension extension

    @Override
    void apply(Project project) {
        extension = project.extensions.create(EXTENSION_NAME, IShallBeReleasedExtension)

        try {
            localGit = Grgit.open(currentDir: "${project.rootDir}")
        } catch (RepositoryNotFoundException e) {
            throw new GradleException("Git repository not found at ${project.rootDir}")
        }

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

        GitHub gh
        try {
            gh = GitHub.connect()
        } catch (IOException ex) {
            throw new GradleException('Failed when trying to connect to GitHub')
        }
        gh.connector = ghConnector

        try {
            ghRepo = gh.getRepository(ghProject)
        } catch (IOException ex) {
            throw new GradleException("Failed when trying to connect to GitHub project ($ghProject)")
        }

        project.version = determineCurrentVersion(localGit)

        configureTasks(project, extension)

    }

    private void configureTasks(final Project project, final IShallBeReleasedExtension extension) {

        addDetermineVersionTask(project, extension)
        addConfigureVersionFileTask(project, extension)
        addDisplayVersionTask(project, extension)

        addCheckReleaseStatusTask(project, extension)
        addPerformReleaseTask(project, extension)

        addGenerateProjectInfoTask(project, extension)
        addDisplayProjectInfoTask(project, extension)
        addConfigurePomTask(project, extension)

        if (project.plugins.hasPlugin(GroovyPlugin)) {
            addJarTasks(project, extension)
        }
    }

    private void addJarTasks(Project project, IShallBeReleasedExtension extension) {
        project.tasks.create(name: 'sourcesJar',
                type: Jar,
                dependsOn: CLASSES_TASK_NAME) {
            classifier = 'sources'
            from project.sourceSets.main.allSource
        }
        project.tasks.create(name: 'testSourcesJar',
                type: Jar,
                dependsOn: TEST_CLASSES_TASK_NAME) {
            classifier = 'test-sources'
            from project.sourceSets.test.allSource
        }
        project.tasks.create(name: 'groovydocJar',
                type: Jar,
                group: 'documentation',
                dependsOn: GROOVYDOC_TASK_NAME) {
            classifier = 'groovydoc'
            from project.groovydoc.destinationDir
        }
    }

    private void addCheckReleaseStatusTask(Project project, IShallBeReleasedExtension extension) {
        project.tasks.create(CHECK_RELEASE_STATUS_TASK_NAME) {
            group = 'release'
            description = 'Checks if there are any items preventing a release.'
            dependsOn GENERATE_PROJECT_INFO_TASK_NAME, BUILD_GROUP, CHECK_TASK_NAME
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
        project.tasks.create(PERFORM_RELEASE_TASK_NAME) {
            group = 'release'
            description = 'Performs the release.'
            dependsOn CHECK_RELEASE_STATUS_TASK_NAME
            doLast {

            }
        }
    }

    private void addDetermineVersionTask(Project project, IShallBeReleasedExtension extension) {
        project.tasks.create(DETERMINE_VERSION_TASK_NAME) {
            group = 'release'
            description = 'Determines the current version.'
            doFirst {
                nextReleaseVersion = determineNextReleaseVersion(project.version)
            }
        }
    }

    private void addConfigureVersionFileTask(Project project, IShallBeReleasedExtension extension) {
        File vFile = project.file("${project.rootDir}/VERSION")
        project.tasks.create(CONFIGURE_VERSION_FILE_TASK_NAME) {
            group = 'release'
            description = 'Adds a VERSION file to the project root'
            outputs.file vFile
            dependsOn DETERMINE_VERSION_TASK_NAME
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
                println "You are working on version ${project.version} and the next release version is ${nextReleaseVersion}"
            }
        }
    }

    private void addGenerateProjectInfoTask(Project project, IShallBeReleasedExtension extension) {
        project.tasks.create(GENERATE_PROJECT_INFO_TASK_NAME) {
            group = 'release'
            description = 'Gathers together various project details.'
            dependsOn DETERMINE_VERSION_TASK_NAME
            doLast {
                if (!extension.projectInfo) {
                    extension.projectInfo = generateProjectInfo(project, ghRepo)
                }
            }
        }
    }

    private void addDisplayProjectInfoTask(Project project, IShallBeReleasedExtension extension) {
        project.tasks.create(DISPLAY_PROJECT_INFO_TASK_NAME) {
            group = 'release'
            description = 'Displays project details.'
            dependsOn GENERATE_PROJECT_INFO_TASK_NAME
            doLast {
                if (extension.projectInfo) {
                    println extension.projectInfo.toYaml()
                } else {
                    throw new GradleException("Task relies on $GENERATE_PROJECT_INFO_TASK_NAME")
                }
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

    static String determineMavenVersion(String version) {
        List<String> components = version.tokenize('-')
        String versionNumber = components.head()
        String postfix = components.size() > 1 ? "-${components.last()}" : ''
        "$versionNumber.0.0$postfix".toString()
    }

    static String determineNextReleaseVersion(String currentVersion,
                                              String releaseTagPrefix = DEFAULT_RELEASE_TAG_PREFIX) {
        "$currentVersion".endsWith("$Snapshot.SNAPSHOT") ? "$currentVersion" - "-$Snapshot.SNAPSHOT" : "$releaseTagPrefix-${currentVersion.tokenize('-')[1] + 1}"
    }

    static ProjectInfo generateProjectInfo(Project project, GHRepository ghRepo) {
        new ProjectInfo(
                name: project.name,
                version: project.version,
                description: project.description ?: ghRepo.description,
                url: ghRepo.homepage.toURL(),
                inceptionYear: new Year(ghRepo.createdAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().year),
                scm: [system             : 'git',
                      url                : ghRepo.gitHttpTransportUrl().toURL(),
                      connection         : "scm:git:${ghRepo.gitHttpTransportUrl()}",
                      developerConnection: "scm:git:${ghRepo.gitHttpTransportUrl()}"] as Scm,
                licenses: [[name: ghRepo.license.name, url: ghRepo.license.url] as License],
                issueManagement: [system: 'GitHub', url: "${ghRepo.htmlUrl}/issues".toURL()] as IssueManagement,
                ciManagement: [system: 'TravisCI', url: "https://travis-ci.org/${ghRepo.fullName}".toURL()] as CiManagement
        )
    }

    private void addConfigurePomTask(Project project, IShallBeReleasedExtension extension) {
        project.tasks.create(CONFIGURE_POM_TASK_NAME) {
            group = 'release'
            description = 'Configures project\'s POM'
            dependsOn GENERATE_PROJECT_INFO_TASK_NAME
            doLast {
                if (!extension.pom) {
                    extension.pom = generatePomNodes(extension.projectInfo)
                }
            }
        }
    }

    Node generatePomNodes(ProjectInfo info) {
        new NodeBuilder().pom {
            name info.name
            description info.description
            url info.url
            inceptionYear info.inceptionYear
            scm {
                url info.scm.url
                connection info.scm.connection
                developerConnection info.scm.developerConnection
            }
            licenses {
                info.licenses.each { License lic ->
                    license {
                        name lic.name
                        url lic.url
                    }
                }
            }
            issueManagement {
                system info.issueManagement.system
                url info.issueManagement.url
            }
            ciManagement {
                system info.ciManagement.system
                url info.ciManagement.url
            }
        }
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
