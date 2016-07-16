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
package com.github.pomes.cli.command

import com.beust.jcommander.Parameters
import com.github.pomes.cli.Context

@Parameters(commandNames = ['repo'],
        resourceBundle = 'com.github.pomes.cli.MessageBundle',
        commandDescriptionKey = 'commandDescriptionRepo')
class CommandRepo implements Command {

    @Override
    Node handleRequest(Context context) {
        new NodeBuilder().repo {
            searching {
                primary displayName: context.searcher.primarySearchProvider.displayName,
                        id: context.searcher.primarySearchProvider.id,
                        url: context.searcher.primarySearchProvider.apiUrl
                secondaries {
                    context.searcher.alternativeSearchProviders.each { provider ->
                        provider.id displayName: provider.displayName,
                                url: provider.apiUrl
                    }
                }
            }
            mavenRepositories {
                local context.resolver.localRepository.basedir
                remotes {
                    context.resolver.remoteRepositories.each { repo ->
                        repository id: repo.id, url: repo.url
                    }
                }
            }
        }
    }
}
