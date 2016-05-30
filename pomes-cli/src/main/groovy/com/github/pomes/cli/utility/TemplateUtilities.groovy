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

class TemplateUtilities {

    static URL getTemplateResource(String baseName, OutputFormat format = OutputFormat.text, Locale locale = Locale.getDefault()) {
        getLocalizedResource(baseName, format.extension, locale)
    }

    static URL getLocalizedResource(String baseName, String suffix, Locale locale = Locale.getDefault()) {
        ResourceBundle.Control control = ResourceBundle.Control.getControl(ResourceBundle.Control.FORMAT_DEFAULT)
        for (Locale specificLocale in control.getCandidateLocales(baseName, locale)) {
            URL url = this.getResource(
                    control.toResourceName(
                            control.toBundleName(baseName, specificLocale),
                            suffix))
            if (url)
                return url
        }
        return null
    }

    static String breakupLongString(String input, Integer maxLineLength = 80, Integer indent = 0) {
        def output = ""
        def currentLine = ""
        for (element in input.tokenize(' ')) {
            if (currentLine.size() + element.size() > maxLineLength - indent) {
                output <<= "${' ' * indent}$currentLine\n"
                currentLine = ""
            }
            currentLine <<= "$element "
        }
        output <<= "${' ' * indent}$currentLine"
        return output
    }
}
