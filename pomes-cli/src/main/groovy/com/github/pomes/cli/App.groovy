package com.github.pomes.cli

import com.beust.jcommander.*
import com.github.pomes.cli.utility.*
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
    MessageBundle versionBundle = new MessageBundle(ResourceBundle.getBundle('com.github.pomes.cli.Version'))

    final String programVersion = versionBundle.getString('version')
    final String programName = bundle.getString('programName')

    Context context

    Configuration configuration

    final JCommander jc

    @Parameter(names = ['-h', '--help'], help = true)
    Boolean help

    @Parameter(names = ['-v', '--version'], descriptionKey = 'parameterVersion')
    Boolean version

    @Parameter(names = ['--logging'], descriptionKey = 'parameterLogging')
    LogLevel logging = LogLevel.error

    @Parameter(names = ['-s', '--settings'], descriptionKey = 'parameterSettings')
    String settings

    @Parameter(names = ['-f', '--format'], descriptionKey = 'parameterFormat')
    OutputFormat format = OutputFormat.text

    App() {
        jc = new JCommander(this)
        jc.programName = programName
        CliCommands.values().each { cmd ->
            jc.addCommand cmd.command
        }
    }

    String getCommand() { jc.parsedCommand }

    void configure(String... args) {
        parse args
        configuration = new Configuration(settings: settings, logging: logging)
        configuration.configure()
    }

    Boolean parse(String... args) {
        try {
            jc.parse(args)
            return true
        } catch (MissingCommandException mce) {
            handleError new Node(null, 'error', [message: bundle.getString('error.missingCommand', args)], mce)
            return false
        } catch (ParameterException pe) {
            handleError new Node(null, 'error', [message: bundle.getString('error.parameterException', args)], pe)
            return false
        }
    }

    Node handleRequest() {
        Node response = new Node(null, 'response')

        if (version) {
            new Node(response, 'version', [name   : programName,
                                           version: programVersion])
            return response
        }

        if (help) {
            StringBuilder usage = new StringBuilder()
            jc.usage(usage)
            new Node(response, 'usage', usage)
            return response
        }

        context = new Context(
                app: this,
                searcher: new Searcher(new JCenter()),
                resolver: new Resolver(configuration.remoteRepositories, configuration.localRepository))

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
        println error.value().class
        if (error.value()?.class in Exception) {
            throw error.value()
        } else {
            System.err.println error.@message
        }
        System.exit(-1)
    }

    void displayResponse(Node response) {
        if (response.error) {
            handleError(response)
        }

        String templateName = command ?: 'base'

        switch (format) {
            case OutputFormat.json:
                print NodeTransformer.nodeToJson(response)
                break
            case OutputFormat.yaml:
                print NodeTransformer.nodeToYaml(response)
                break
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
            cli.configure(args)
            cli.displayResponse cli.handleRequest()
        } catch (any) {
            cli.handleError new Node(null, 'error', [message: cli.bundle.getString('error.unhandledException', any.message)], any)
        }
    }

}
