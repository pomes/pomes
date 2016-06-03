package com.github.pomes.cli.command

import com.beust.jcommander.Parameters
import com.github.pomes.cli.Context
import com.github.pomes.cli.utility.MessageBundle
import com.github.pomes.core.Resolver
import com.github.pomes.core.Searcher
import com.github.pomes.core.repositories.JCenter
import com.github.pomes.core.repositories.MavenCentral
import groovy.util.logging.Slf4j

@Slf4j
@Parameters(commandNames = ['about'], resourceBundle = 'com.github.pomes.cli.MessageBundle', commandDescriptionKey = 'commandDescriptionAbout')
class CommandAbout implements Command {

    @Override
    Node handleRequest(Context context) {
        MessageBundle bundle = context.app.bundle
        new NodeBuilder().about {
            program {
                name context.app.programName
                version context.app.programVersion
                licence bundle.getString('programLicence')
            }
            thirdPartyLibraries {
                maven(name: 'Apache Maven', url: 'http://maven.apache.org/', licence: 'Apache License - v 2.0')
                aether(name: 'Eclipse Aether', url: 'https://www.eclipse.org/aether/', licence: 'Eclipse Public License - v 1.0')
                //dependencyCheck(name: 'OWASP Dependency Check', url: 'https://www.owasp.org/index.php/OWASP_Dependency_Check', licence: 'Apache License - v 2.0')
            }
            thirdPartyServices {
                jcenter(name: JCenter.displayName, url: JCenter.url)
                mavenCentral(name: MavenCentral.displayName, url: MavenCentral.url)
            }
        }
    }
}
