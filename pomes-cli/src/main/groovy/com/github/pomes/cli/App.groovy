package com.github.pomes.cli

import com.beust.jcommander.*
import com.github.pomes.cli.utility.MessageBundle
import com.github.pomes.cli.utility.OutputFormat
import com.github.pomes.cli.utility.TemplateUtilities
import com.github.pomes.core.Resolver
import com.github.pomes.core.Searcher
import com.github.pomes.core.repositories.JCenter
import groovy.text.GStringTemplateEngine
import groovy.util.logging.Slf4j
import groovy.xml.XmlUtil

import java.nio.file.Files
import java.util.ResourceBundle

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

@Slf4j
@Parameters(resourceBundle = 'com.github.pomes.cli.MessageBundle')
class App {

    MessageBundle bundle = new MessageBundle(ResourceBundle.getBundle('com.github.pomes.cli.MessageBundle'))

    final String programVersion = bundle.getString('programVersion')
    final String programName = bundle.getString('programName')

    Context context

    @Parameter(names = ['-h', '--help'], help = true)
    Boolean help

    @Parameter(names = ['-v', '--version'], descriptionKey = 'parameterVersion')
    Boolean version

    @Parameter(names = ['-s', '--settings'], descriptionKey = 'parameterSettings')
    String settings

    @Parameter(names = ['-f', '--format'], descriptionKey = 'parameterFormat')
    OutputFormat format = OutputFormat.text

    final Defaults defaults = new Defaults()

    final JCommander jc

    App() {
        jc = new JCommander(this)
        jc.programName = programName
        CliCommands.values().each { cmd ->
            jc.addCommand cmd.command
        }
    }

    Boolean parse(String... args) {
        try {
            jc.parse(args)
            return true
        } catch (MissingCommandException mce) {
            handleError new Node(null, 'error', [message: bundle.getString('error.missingCommand', args)], mce.message)
            return false
        } catch (ParameterException pe) {
            handleError new Node(null, 'error', [message: bundle.getString('error.parameterException', args)], pe.message)
            return false
        }
    }

    String getCommand() { jc.parsedCommand }

    Node handleRequest() {
        Node response = new Node(null, 'response')

        if (version) {
            new Node(response, 'version', [name   : bundle.getString('programName'),
                                           version: bundle.getString('programVersion')])
            return response
        }

        if (help) {
            StringBuilder usage = new StringBuilder()
            jc.usage(usage)
            new Node(response, 'usage', usage)
            return response
        }

        context = new Context(
                jCommander: jc,
                searcher: new Searcher(new JCenter()),
                resolver: new Resolver(defaults.remoteRepositories, defaults.localRepository))

        if (!Files.exists(context.resolver.localRepository.basedir.toPath())) {
            log.info bundle.getString('log.creatingLocalRepository', context.resolver.localRepository.basedir)
            Files.createDirectories(context.resolver.localRepository.basedir.toPath())
        }

        CliCommands command = CliCommands.lookupCliCommand(this.command)

        if (command?.command) {
            log.debug "Calling command: $command.value"
            response.append command.command.handleRequest(context)
        } else {
            new Node(response, 'error', [message: bundle.getString('error.commandNotFound')])
        }
        return response
    }

    void handleError(Node response) {
        Node error = response.error ? response.error[0] : response
        String details = ''

        if (error.value()?.class in Exception) {
            details = error.value().cause ?: "Stacktrace: ${error.value().stackTrace.toString()}"
        } else if (error.value()) {
            details = error.value().toString()
        }

        log.error "${error.@message}. ${details}"
        System.err.println error.@message
        System.exit(-1)
    }

    void displayResponse(Node response) {
        if (response.error) {
            handleError(response)
        }

        String templateName = command ?: 'base'

        switch (format) {
            case OutputFormat.xml:
                print XmlUtil.serialize(response)
                break
            case OutputFormat.raw:
                print response.toString()
                break
            case OutputFormat.text:
            case OutputFormat.html:
            default:
                String templatePath = "/com/github/pomes/cli/templates/command/$templateName/$templateName".toString()

                log.info bundle.getString('log.proposedTemplate', templatePath)
                URL template = TemplateUtilities.getTemplateResource(templatePath, format)

                if (!template && format == OutputFormat.html) {
                    //try for a text template
                    log.info bundle.getString('log.failoverToTextTemplate')
                    template = TemplateUtilities.getTemplateResource(templatePath, OutputFormat.text)
                }

                if (template) {
                    log.info bundle.getString('log.selectedTemplate', template)
                    GStringTemplateEngine engine = new GStringTemplateEngine()

                    print engine.createTemplate(template).
                            make(context: context,
                                    response: response,
                                    utilities: [breakupLongString: TemplateUtilities.&breakupLongString]).
                            toString()
                } else {
                    new Node(response, 'error', [message: bundle.getString('error.templateNotFound', templatePath)])
                    handleError(response)
                }
        }
        System.exit(0)
    }

    static void main(String[] args) {
        App cli = new App()
        try {
            cli.parse(args)
            cli.displayResponse cli.handleRequest()
        } catch (any) {
            cli.handleError new Node(null, 'error', [message: cli.bundle.getString('error.unhandledException', any.message)], any)
        }
    }

}
