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

package com.github.pomes.cli.utility

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.FileAppender
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy
import com.github.pomes.core.repositories.DefaultLocalRepository
import com.github.pomes.core.repositories.JCenter
import groovy.util.logging.Slf4j
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.slf4j.LoggerFactory

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Slf4j
class Configuration {
    MessageBundle bundle = new MessageBundle(ResourceBundle.getBundle('com.github.pomes.cli.MessageBundle'))

    final String programName = bundle.getString('programName')

    final Path configurationFile

    Path applicationHome

    final LogLevel logLevel

    final List<RemoteRepository> remoteRepositories = []

    LocalRepository localRepository

    Configuration(Map config = [:]) {
        this.configurationFile = config?.settings? Paths.get(settings):
                Paths.get(System.getProperty('user.home'), ".${bundle.getString('programName')}")

        this.logLevel = config?.logging?: LogLevel.error
    }


    void configure() {
        applicationHome = Paths.get(System.getProperty('user.home'), '.pomes')

        remoteRepositories?: remoteRepositories << JCenter.newJCenterRemoteRepository()
        remoteRepositories.asImmutable()

        localRepository = new LocalRepository(applicationHome.resolve('repository').toFile(), null)
        createHomeDirectoryStructure()
        configureLogging()
    }

    private void createHomeDirectoryStructure() {
        Files.createDirectories(applicationHome)
        Files.createDirectories(localRepository.basedir.toPath())
    }

    private void configureLogging() {
        //Setup logging
        Logger rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
        LoggerContext loggerContext = LoggerFactory.getILoggerFactory()

        rootLogger.level = logLevel.level

        PatternLayoutEncoder encoder = new PatternLayoutEncoder()
        encoder.context = loggerContext
        encoder.pattern = '%-12date{YYYY-MM-dd HH:mm:ss.SSS} [%thread] %c %-5level - %msg%n'
        encoder.start()

        FileAppender appender = new FileAppender()
        appender.context = loggerContext
        appender.name = "${programName}Log"
        appender.file = applicationHome.resolve("${programName}.log")
        appender.append = true
        appender.encoder = encoder

        TimeBasedRollingPolicy logPolicy = new TimeBasedRollingPolicy()
        logPolicy.context = loggerContext
        logPolicy.parent = appender
        logPolicy.fileNamePattern = 'logs/logfile-%d{yyyy-MM-dd_HH}.log'
        logPolicy.maxHistory = 7
        logPolicy.start()

        rootLogger.detachAndStopAllAppenders()

        appender.start()
        rootLogger.addAppender(appender)
    }
}
