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

package com.github.pomes.core

import groovy.transform.Immutable
import groovy.util.logging.Slf4j
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Helper functions
 *
 * @see <a href="https://github.com/shrinkwrap/resolver">https://github.com/shrinkwrap/resolver</a>
 */
@Slf4j
@Immutable(copyWith = true)
class ArtifactCoordinate {
    /** Used to parse a Maven  groupId:artifactId[:packaging][:classifier]:version */
    static final Pattern COORDINATE_PATTERN = ~/([^: ]+):([^: ]+)(:([^: ]*)(:([^: ]+))?)?(:([^: ]+))?/
    static final Integer COORDINATE_PATTERN_POS_GROUP = 1
    static final Integer COORDINATE_PATTERN_POS_ARTIFACT = 2
    static final Integer COORDINATE_PATTERN_POS_3 = 4
    static final Integer COORDINATE_PATTERN_POS_4 = 6
    static final Integer COORDINATE_PATTERN_POS_5 = 8

    static final String VERSION_OPEN = '[0,)'

    String groupId, artifactId, version, extension, classifier

    @Override
    String toString() {
        "$groupId:$artifactId${extension?":$extension":''}${classifier?":$classifier":''}:$version"
    }

    Artifact getArtifact() {
        if (!extension)
            return new DefaultArtifact(toString())

        if (!classifier)
            return new DefaultArtifact(groupId, artifactId, extension, version)

        return new DefaultArtifact(groupId, artifactId, classifier, extension, version)
    }

    /**
     * Parses a Maven coordinate
     *
     * @see <a href="https://maven.apache.org/pom.html#Maven_Coordinates">Maven coordinates</a>
     *
     * @param coordinate
     * @return
     */
    static ArtifactCoordinate parseCoordinates(String coordinate) {
        log.debug "Instantiating new ArtifactCoordinate from $coordinate"

        String groupId, artifactId, version, extension, classifier
        (groupId, artifactId, version, extension, classifier) = ['', '', '', '', '']

        Matcher matcher = COORDINATE_PATTERN.matcher(coordinate)

        if (!matcher.matches()) {
            log.warn "Could not parse the coordinate $coordinate"
            throw new IllegalArgumentException("Could not parse the coordinate $coordinate")
        }
        groupId = matcher.group(COORDINATE_PATTERN_POS_GROUP)
        artifactId = matcher.group(COORDINATE_PATTERN_POS_ARTIFACT)

        switch (coordinate.count(':')) {
            case 2:
                //Normal GAV
                version = matcher.group(COORDINATE_PATTERN_POS_3)
                break
            case 3:
                //GAV + extension
                extension = matcher.group(COORDINATE_PATTERN_POS_3)
                version = matcher.group(COORDINATE_PATTERN_POS_4)
                break
            case 4:
                //GAV + extension + classifier
                extension = matcher.group(COORDINATE_PATTERN_POS_3)
                classifier = matcher.group(COORDINATE_PATTERN_POS_4)
                version = matcher.group(COORDINATE_PATTERN_POS_5)
                break
        }

        //Just assume we're after the POM
        //if (!extension) extension = ArtifactExtension.POM.value

        ArtifactCoordinate ac = new ArtifactCoordinate(groupId, artifactId, version, extension, classifier)
        log.debug "New ArtifactCoordinate from $coordinate: $ac"
        return ac
    }
}
