package org.rundeck.plugins.nodes.icon;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.DynamicProperties;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.descriptions.SelectLabels;
import com.dtolabs.rundeck.plugins.descriptions.SelectValues;
import com.dtolabs.rundeck.plugins.nodes.IModifiableNodeEntry;
import com.dtolabs.rundeck.plugins.nodes.NodeEnhancerPlugin;
import org.rundeck.app.spi.Services;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Plugin(service = "NodeEnhancer", name = IconNodeEnhancer.PROVIDER)
@PluginDescription(title = "Icon",
                   description =
                           "Adds an icon or badges based on an attribute.\n\nIf the attribute exists and optionally "
                           + "matches a certain value, use the selected icon")
public class IconNodeEnhancer
        implements NodeEnhancerPlugin, DynamicProperties
{

    private static final String[] ALL_NAMES;

    static {
        Properties iconProperties = new Properties();
        try {
            InputStream resourceAsStream = IconNodeEnhancer.class
                    .getClassLoader()
                    .getResourceAsStream("org/rundeck/plugins/nodes/icon/IconNodeEnhancer.properties");
            iconProperties.load(resourceAsStream);
            String[] GLYPHICON_NAMES = iconProperties.getProperty("glyphicon.names").split(",");
            for (int i = 0; i < GLYPHICON_NAMES.length; i++) {
                GLYPHICON_NAMES[i] = "glyphicon-" + GLYPHICON_NAMES[i];
            }
            String[] FAICON_NAMES = iconProperties.getProperty("faicon.names").split(",");
            for (int i = 0; i < FAICON_NAMES.length; i++) {
                FAICON_NAMES[i] = "fa-" + FAICON_NAMES[i];
            }
            String[] all = new String[GLYPHICON_NAMES.length + FAICON_NAMES.length];
            System.arraycopy(GLYPHICON_NAMES, 0, all, 0, GLYPHICON_NAMES.length);
            System.arraycopy(FAICON_NAMES, 0, all, GLYPHICON_NAMES.length, FAICON_NAMES.length);
            ALL_NAMES = all;
        } catch (IOException e) {
            throw new RuntimeException("Unable to load resource", e);
        }
    }

    public static final String PROVIDER = "icon";

    @PluginProperty(title = "Attribute Name", description = "Attribute name to look for", required = true)
    private String attributeName;
    @PluginProperty(title = "Attribute Value",
                    description = "Exact attribute value to match (optional)",
                    required = false)
    private String attributeValue;

    /**
     *
     */
    @PluginProperty(title = "Icon Name",
                    description =
                            "Icon name to use, glyphicons start with `glyphicon-` and font-awesome icons start with "
                            + "`fa-`, and font-awesome Brand icons start with `fab-`.\n\nSee [Font-Awesome]"
                            + "(https://fontawesome"
                            + ".com/icons?d=gallery&m=free) and [Glyphicon](https://getbootstrap.com/docs/3"
                            + ".4/components/)")

    private String iconName;
    /**
     *
     */
    @PluginProperty(title = "Icon Color", description = "CSS color for the icon (optional)")
    private String iconColor;
    /**
     *
     */
    @PluginProperty(title = "Badges",
                    description = "Icon badges to add to the Node. Each badge should start with `fa-` or `fab-` or "
                                  + "`glyphicon-`.")
    private List<String> iconBadges;

    @Override
    public Map<String, Object> dynamicProperties(
            final Map<String, Object> projectAndFrameworkValues,
            final Services services
    )
    {
        HashMap<String, Object> values = new HashMap<>();
        List<String> iconBadges = Arrays.asList(ALL_NAMES);
        values.put("iconName", iconBadges);
        return values;
    }

    @Override
    public void updateNode(
            final String project, final IModifiableNodeEntry node
    )
    {
        if (!node.getAttributes().containsKey(attributeName)) {
            return;
        }
        if (attributeValue != null
            && !"".equals(attributeValue)
            && !attributeValue.equals(node.getAttributes().get(attributeName))) {
            return;
        }
        node.addAttribute("ui:icon:name", iconName);
        if (iconColor != null && !"".equals(iconColor)) {
            node.addAttribute("ui:icon:color", iconColor);
        }
        if (iconBadges != null && iconBadges.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (String iconBadge : iconBadges) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                if (iconBadge.startsWith("glyphicon-") || iconBadge.startsWith("fa-") || iconBadge.startsWith("fab-")) {
                    sb.append(iconBadge);
                }
            }
            node.addAttribute("ui:badges", sb.toString());
        }
    }

}
