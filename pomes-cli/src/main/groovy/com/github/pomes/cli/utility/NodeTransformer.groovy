package com.github.pomes.cli.utility

import groovy.json.JsonBuilder
import org.yaml.snakeyaml.Yaml

class NodeTransformer {

    /**
     *
     * @see <a href='http://stackoverflow.com/questions/18830248/converting-xml-to-json-in-groovy'>Stack Overflow answer by Tim Yates</a>
     *
     * @param node
     * @return
     */
    static String nodeToJson(Node node) {
        new groovy.json.JsonBuilder(nodeToMap(node)).toPrettyString()
    }

    static String nodeToYaml(Node node) {
        new Yaml().dump(nodeToMap(node))
    }

    static Map nodeToMap(Node node) {
        def handle
        handle = { n ->
            if (n in String)
                return n
            Map retMap = n.attributes()?:[:]
            List values = n.collect(handle)
            if (values) {
                retMap << [values: values]
            }
            [(n.name()): retMap]
        }

        // Convert it to a Map containing a List of Maps
        [(node.name()): node.collect { nodeChild -> [(nodeChild.name()): nodeChild.collect(handle)] }]
    }
}
