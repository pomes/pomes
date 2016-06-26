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

import groovy.transform.ToString
import org.yaml.snakeyaml.Yaml

import java.time.Year

@ToString(includeNames = true)
class ProjectInfo {
    String name, description
    URL url
    Year inceptionYear
    Scm scm
    List<License> licenses
    IssueManagement issueManagement
    CiManagement ciManagement

    String toYaml() {
        //Try: https://github.com/EsotericSoftware/yamlbeans ?
        Yaml yaml = new Yaml()
        yaml.dump(this)
    }

    @ToString(includeNames = true)
    class Scm {
        String system
        URL url
        String connection, developerConnection
    }

    @ToString(includeNames = true)
    class License  {
        String system
        URL url
    }

    @ToString(includeNames = true)
    class IssueManagement  {
        String system
        URL url
    }

    @ToString(includeNames = true)
    class CiManagement  {
        String system
        URL url
    }
}

