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

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.github.pomes.core.ArtifactCoordinate
import com.github.pomes.core.Resolver
import com.github.pomes.core.Searcher
import groovy.util.logging.Slf4j
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.collection.CollectResult
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.graph.DependencyNode
import org.eclipse.aether.graph.DependencyVisitor

import static org.eclipse.aether.util.artifact.JavaScopes.COMPILE

@Slf4j
@Parameters(commandNames = ['dependencies'], commandDescription = "Displays dependency information for an artifact")
class CommandDependencies implements Command {
    @Parameter(description = '<coordinates>')
    List<String> coordinates

    @Parameter(names = ['-l', '--latest'], description = 'Use the latest version')
    Boolean latest

    @Parameter(names = ['-s', '--scope'], description = 'Sets the dependency scope')
    String scope = COMPILE

    @Parameter(names = ['-t', '--transitive'], description = 'Resolves transitive dependencies')
    Boolean transitive = false

    @Override
    void handleRequest(Searcher searcher, Resolver resolver) {
        coordinates.each { coordinate ->
            log.debug "Dependencies request for $coordinate (latest requested: $latest)"
            ArtifactCoordinate ac = ArtifactCoordinate.parseCoordinates(coordinate)

            if (latest) {
                ac = ac.copyWith(version: resolver.getArtifactLatestVersion(ac))
            }

            println "Dependencies for $ac (scope: $scope)"

            CollectResult collectResult = resolver.collectAllDependencies(ac.artifact, scope)
            log.debug "Dependency root: ${collectResult.root.artifact}"
            //collectResult.root.accept(new DependencyGraphDumper())
        }
    }

    /**
     * Based on http://git.eclipse.org/c/aether/aether-demo.git/tree/aether-demo-snippets/src/main/java/org/eclipse/aether/examples/util/ConsoleDependencyGraphDumper.java
     */
    /*
    class DependencyGraphDumper
            implements DependencyVisitor {

        private PrintStream out

        private List<ChildInfo> childInfos = []

        @Override
        boolean visitEnter(DependencyNode node) {
            out.println "${formatIndentation()}${formatNode(node)}"
            childInfos << new ChildInfo(node.children.size())
            true
        }

        @Override
        boolean visitLeave(DependencyNode node) {
            if (childInfos) {
                childInfos.remove(childInfos.size() - 1)
            }
            if (childInfos) {
                childInfos.get(childInfos.size() - 1).index++
            }
            true
        }

        private String formatNode(DependencyNode node) {
            StringBuilder buffer = new StringBuilder(128);

            Artifact a = node.artifact
            Dependency d = node.dependency

            buffer <<= a
            if (d?.scope.length()) {
                buffer <<= " [$d.scope${d.isOptional()?', optional':''}]"
            }

            {
                String premanaged = DependencyManagerUtils.getPremanagedVersion(node);
                if (premanaged != null && !premanaged.equals(a.getBaseVersion())) {
                    buffer.append(" (version managed from ").append(premanaged).append(")");
                }
            }
                    {
                        String premanaged = DependencyManagerUtils.getPremanagedScope(node);
                        if (premanaged != null && !premanaged.equals(d.getScope())) {
                            buffer.append(" (scope managed from ").append(premanaged).append(")");
                        }
                    }
            DependencyNode winner = (DependencyNode) node.getData().get(ConflictResolver.NODE_DATA_WINNER);
            if (winner != null && !ArtifactIdUtils.equalsId(a, winner.getArtifact())) {
                Artifact w = winner.getArtifact();
                buffer.append(" (conflicts with ");
                if (ArtifactIdUtils.toVersionlessId(a).equals(ArtifactIdUtils.toVersionlessId(w))) {
                    buffer.append(w.getVersion());
                } else {
                    buffer.append(w);
                }
                buffer.append(")");
            }
            return buffer.toString();
        }

        private String formatIndentation() {
            StringBuilder buffer = new StringBuilder(128);
            for (Iterator<ChildInfo> it = childInfos.iterator(); it.hasNext();) {
                buffer.append(it.next().formatIndentation(!it.hasNext()));
            }
            return buffer.toString();
        }
    }

    private static class ChildInfo {

        final int count;

        int index;

        public ChildInfo(int count) {
            this.count = count;
        }

        public String formatIndentation(boolean end) {
            boolean last = index + 1 >= count;
            if (end) {
                return last ? "\\- " : "+- ";
            }
            return last ? "   " : "|  ";
        }

    }
    */
}
