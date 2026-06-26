package org.rundeck.plugins.nodes.attributes

import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.plugins.nodes.IModifiableNodeEntry
import spock.lang.Specification

class AttributeNodeEnhancerSpec extends Specification{

    def "test bad regex match"(){

        given:
        def plugin = new AttributeNodeEnhancer()
        plugin.match = "attb1=value1"
        plugin.add = "attb2=value2"


        def node = new ModifiableNodeEntry("test1")
        node.attributes = [attb1:"value1"]

        def project = "TestProject"
        when:

        plugin.updateNode(project, node)

        then:
        node.attributes.attb2 == null

    }

    def "test regex match"(){

        given:
        def plugin = new AttributeNodeEnhancer()
        plugin.match = "attb1==value1"
        plugin.add = "attb2=value2"


        def node = new ModifiableNodeEntry("test1")
        node.attributes = [attb1:"value1"]

        def project = "TestProject"
        when:

        plugin.updateNode(project, node)

        then:
        node.attributes.attb2 == "value2"

    }

    def "test attribute value substitution"() {

        given:
        def plugin = new AttributeNodeEnhancer()
        plugin.match = "attb1==value1\nattb2~~"
        plugin.add = 'attb2=a-${attb1}-b-${attb2}'
        plugin.enableSubstitution = enabled


        def node = new ModifiableNodeEntry("test1")
        node.attributes = [attb1: "value1", attb2: "value2"]

        def project = "TestProject"
        when:

        plugin.updateNode(project, node)

        then:
        node.attributes.attb2 == result

        where:
        enabled | result
        true    | "a-value1-b-value2"
        false   | 'a-${attb1}-b-${attb2}'
    }

    def "test tag substitution"() {

        given:
        def plugin = new AttributeNodeEnhancer()
        plugin.match = "attb1==value1\nattb2~~"
        plugin.addTags = 'tag1,a-${attb1}-b-${attb2}'
        plugin.enableSubstitution = enabled


        def node = new ModifiableNodeEntry("test1")
        node.attributes = [attb1: "value1", attb2: "value2"]
        node.tags = new HashSet<>(["tag0"])

        def project = "TestProject"
        when:

        plugin.updateNode(project, node)

        then:
        node.tags == ["tag0", "tag1", result].toSet()

        where:
        enabled | result
        true    | "a-value1-b-value2"
        false   | 'a-${attb1}-b-${attb2}'

    }

    def "test multiples regex match"(){

        given:
        def plugin = new AttributeNodeEnhancer()
        plugin.match = match
        plugin.add = "result=valueResult"


        def node = new ModifiableNodeEntry("test1")
        node.attributes = [attb1:"value1",attb2:"value2"]

        def project = "TestProject"
        when:

        plugin.updateNode(project, node)

        then:
        node.attributes.result == result

        where:
        match                                   | result
        "attb1==value1\nattb2==value2"          | "valueResult"
        "attb1==value1\r\nattb2==value2"        | "valueResult"
        "attb1=value1\r\nattb2==value2"         | null
        "attb1==value1\nattb2=value2"           | null
    }

    def "test present and not present match"() {

        given:
        def plugin = new AttributeNodeEnhancer()
        plugin.match = match
        plugin.add = "result=valueResult"


        def node = new ModifiableNodeEntry("test1")
        node.attributes = [attb1: "value1", attb2: "value2"]

        def project = "TestProject"
        when:

        plugin.updateNode(project, node)

        then:
        node.attributes.result == result

        where:
        match     | result
        "attb1~~" | "valueResult"
        "attb1!!" | null
        "attb2~~" | "valueResult"
        "attb2!!" | null
        "attb3~~" | null
        "attb3!!" | "valueResult"
    }


    class ModifiableNodeEntry extends NodeEntryImpl implements IModifiableNodeEntry{

        ModifiableNodeEntry(final String nodename) {
            super(nodename)
        }


        @Override
        void addAttribute(final String name, final String value) {
            attributes.put(name, value)
        }

        @Override
        void removeAttribute(final String name) {
            attributes.remove(name)
        }

        @Override
        void addTag(final String tag) {
            tags.add(tag)
        }

        @Override
        void removeTag(String tag) {
            tags.remove(tag)
        }
    }

}
