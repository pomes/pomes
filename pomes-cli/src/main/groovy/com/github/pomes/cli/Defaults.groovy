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

package com.github.pomes.cli

import com.github.pomes.core.repositories.DefaultLocalRepository
import com.github.pomes.core.repositories.JCenter
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository

import java.nio.file.Path
import java.nio.file.Paths

class Defaults {
    final ResourceBundle bundle = ResourceBundle.getBundle('com.github.pomes.cli.MessageBundle')

    final Path applicationDirectory = Paths.get(System.getProperty('user.home'), ".${bundle.getString('programName')}")

    final List<RemoteRepository> remoteRepositories = [JCenter.newJCenterRemoteRepository()]

    final LocalRepository localRepository = DefaultLocalRepository.repository
    
}
