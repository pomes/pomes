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

enum OutputFormat {
    text('text', 'txt'),
    xml('xml', 'xml'),
    html('html', 'html'),
    raw('raw', 'node')

    //YAML('yaml', 'yaml'),
    //JSON('json', 'json'),

    String value, extension

    OutputFormat(String value, String extension) {
        this.value = value
        this.extension = extension
    }
}
