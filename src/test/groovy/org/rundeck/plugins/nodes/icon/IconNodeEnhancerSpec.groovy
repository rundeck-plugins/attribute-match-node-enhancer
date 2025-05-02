package org.rundeck.plugins.nodes.icon

import com.dtolabs.rundeck.plugins.nodes.IModifiableNodeEntry
import spock.lang.Specification

class IconNodeEnhancerSpec extends Specification {

    def "test icon enhancement"() {
        given:
        def node = Mock(IModifiableNodeEntry) {
            getAttributes() >> ['key': 'value']
        }
        def iconEnhancer = new IconNodeEnhancer()
        iconEnhancer.attributeName = attributeName
        iconEnhancer.attributeValue = attributeValue
        iconEnhancer.iconName = iconName
        iconEnhancer.iconColor = iconColor
        iconEnhancer.iconBadges = iconBadges
        when:
        iconEnhancer.updateNode('project', node)

        then:
        (expectName ? 1 : 0) * node.addAttribute('ui:icon:name', expectName)
        (expectColor ? 1 : 0) * node.addAttribute('ui:icon:color', expectColor)
        (expectBadges ? 1 : 0) * node.addAttribute('ui:badges', expectBadges)
        0 * node.addAttribute(_, null)

        where:
        attributeName | attributeValue | iconName         | iconColor   | iconBadges                  | expectName       | expectColor | expectBadges
        'key'         | 'value'        | null             | null        | []                          | null             | null        | null
        'wrongkey'    | 'value'        | 'fa-iconName'    | 'iconColor' | ['fa-badge1', 'fab-badge2'] | null             | null        | null
        'key'         | 'value'        | null             | null        | ['fa-badge1', 'fab-badge2'] | null             | null        | 'fa-badge1,fab-badge2'
        'key'         | 'value'        | null             | 'iconColor' | []                          | null             | 'iconColor' | null
        'key'         | 'value'        | 'fa-test'        | null        | []                          | 'fa-test'        | null        | null
        'key'         | 'value'        | 'fab-test'       | null        | []                          | 'fab-test'       | null        | null
        'key'         | 'value'        | 'glyphicon-test' | null        | []                          | 'glyphicon-test' | null        | null
        'key'         | 'value'        | 'other'          | null        | []                          | null             | null        | null
        'key'         | 'value'        | 'fa-test'        | 'iconColor' | ['fa-badge1', 'fab-badge2'] | 'fa-test'        | 'iconColor' | 'fa-badge1,fab-badge2'
        'key'         | 'value'        | 'fa-test'        | 'iconColor' | ['wrong', 'fab-badge2']     | 'fa-test'        | 'iconColor' | 'fab-badge2'
        'key'         | 'value'        | 'fa-test'        | 'iconColor' | ['glyphicon-x']             | 'fa-test'        | 'iconColor' | 'glyphicon-x'
    }
}
