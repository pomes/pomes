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

package com.github.pomes.core.dependency.graph.display

import com.github.pomes.core.Resolver
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.graph.DependencyNode
import org.eclipse.aether.graph.DependencyVisitor
import org.eclipse.aether.util.artifact.ArtifactIdUtils
import org.eclipse.aether.util.graph.transformer.ConflictResolver

import static org.eclipse.aether.util.graph.manager.DependencyManagerUtils.getPremanagedScope
import static org.eclipse.aether.util.graph.manager.DependencyManagerUtils.getPremanagedVersion

/**
 * Based on http://git.eclipse.org/c/aether/aether-demo.git/tree/aether-demo-snippets/src/main/java/org/eclipse/aether/examples/util/ConsoleDependencyGraphDumper.java
 */
class CommandLineDumperTransitiveDependencyCheck
        implements DependencyVisitor {

    private PrintStream out

    private List<ChildInfo> childInfos = []

    private Resolver resolver

    CommandLineDumperTransitiveDependencyCheck(Resolver resolver) {
        this.resolver = resolver
    }

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
        StringBuilder buffer = new StringBuilder(128)

        Artifact a = node.artifact
        Dependency d = node.dependency

        buffer <<= a
        if (d?.scope?.length()) {
            buffer <<= " [$d.scope${d.isOptional() ? ', optional' : ''}]"
        }

        String premanagedVersion = getPremanagedVersion(node)
        if (premanagedVersion && premanagedVersion != a.baseVersion) {
            buffer <<= " (version managed from $premanagedVersion)"
        }

        String premanagedScope = getPremanagedScope(node)
        if (premanagedScope && premanagedScope != d.scope) {
            buffer <<= " (scope managed from $premanagedScope)"
        }

        DependencyNode winner = (DependencyNode) node.data.get(ConflictResolver.NODE_DATA_WINNER)
        if (winner && !ArtifactIdUtils.equalsId(a, winner.artifact)) {
            Artifact w = winner.artifact
            buffer <<= ' (conflicts with '
            buffer <<= (ArtifactIdUtils.toVersionlessId(a) == ArtifactIdUtils.toVersionlessId(w)) ? w.version : w
            buffer <<= ')'
        }
        String latestVersion = resolver.getArtifactLatestVersion(a)
        if (a.version != latestVersion) {
            buffer <<= "[Outdated: $latestVersion]"
        }

        buffer.toString()
    }

    private String formatIndentation() {
        StringBuilder buffer = new StringBuilder(128)
        childInfos.eachWithIndex{ ChildInfo entry, int i ->
            buffer <<= entry.formatIndentation(i == childInfos.size() - 1)
        }
        buffer.toString()
    }


    private static class ChildInfo {

        final int count
        int index

        ChildInfo(int count) {
            this.count = count
        }

        String formatIndentation(boolean end) {
            boolean last = index + 1 >= count
            if (end) {
                return last ? '\\- ' : '+- '
            }
            return last ? '   ' : '|  '
        }
    }
}
