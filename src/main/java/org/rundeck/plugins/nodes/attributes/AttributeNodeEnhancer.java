package org.rundeck.plugins.nodes.attributes;

import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginMetadata;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.descriptions.TextArea;
import com.dtolabs.rundeck.plugins.nodes.IModifiableNodeEntry;
import com.dtolabs.rundeck.plugins.nodes.NodeEnhancerPlugin;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Plugin(service = "NodeEnhancer", name = org.rundeck.plugins.nodes.attributes.AttributeNodeEnhancer.PROVIDER)
@PluginDescription(title = "Attribute Match",
                   description =
                           "Adds new attributes to a node if a match is found.\n\nIf the attribute exists and "
                           + "optionally "
                           + "matches a certain value, add the custom attributes.")
@PluginMetadata(key = "faicon", value = "list-alt")
public class AttributeNodeEnhancer
        implements NodeEnhancerPlugin
{
    public static final String PROVIDER = "attributeMatch";


    @PluginProperty(title = "Attribute Matches",
                    description =
                            "Attribute name/values to match. All matches must be met to add the additional attributes"
                            + ".\n\n"
                            + "Each entry should be of the form:\n"
                            + "* `key` `operator` `value`\n\n"
                            + "Where `key` is the attribute name, "
                            + "`operator` is one of:\n\n"
                            + "* `==` equality match\n"
                                    + "* `~~` is present match (no value)\n"
                                    + "* `!!` not present match (no value)\n"
                            + "* `=~` regular expression match\n"
                            + "* `!=` inequality match\n"
                                    + "* `!~` negative regular expression match\n\n"
                                    + "`value` is optional for some operators.\n",
                    required = true)
    @TextArea
    private String match;

    /**
     *
     */
    @PluginProperty(title = "Attributes to Add",
                    description = "Attribute name=value to add to matching nodes, in Java Properties format")
    @TextArea
    private String add;

    @PluginProperty(title = "Tags to Add",
                    description = "Comma-separated tags to add")

    private String addTags;

    @PluginProperty(title = "Enable Attribute Substitution",
            description = "If enabled, added tags and attribute values can use `${attribute}` to substitute existing node attribute values. E.g. `tag1,image-${ec2.imageId}` or `newattr=some-${oldattr}/${otherattr}`")
    private boolean enableSubstitution;


    static final Pattern ComparisonPattern = Pattern.compile(
            "^(?<key>.+?)(?<op>==|!=|=~|!~|!!|~~)(?<val>.*)$"
    );

    @Override
    public void updateNode(
            final String project, final IModifiableNodeEntry node
    )
    {
        if (!matchesAll(node.getAttributes(), match)) {
            return;
        }
        addAll(node.getAttributes(), add, enableSubstitution);
        addAllTags(node.getTags(), node.getAttributes(), addTags, enableSubstitution);
    }

    public static void addAllTags(final Set<String> tags, Map<String, String> attributes, String newTags, boolean enableSubstitution) {
        Set<String> loadedTags = new HashSet<>();
        if (newTags != null && !newTags.trim().isEmpty()) {
            String[] list = newTags.split("\\s*,\\s*");
            for (String tag : list) {
                if (!tag.trim().isEmpty()) {
                    //substitute any existing attribute values
                    loadedTags.add(
                            enableSubstitution ? substitute(attributes, tag)
                                    : tag
                    );
                }
            }
        }
        tags.addAll(loadedTags);
    }


    static final Pattern PROP_REF_PATTERN = Pattern.compile("\\$\\{(?<name>[^}]+)}");

    public static void addAll(final Map<String, String> attributes, String propString, boolean enableSubstitution) {
        Map<String, String> map = new HashMap<>();
        if (propString != null && !propString.trim().isEmpty()) {
            try {
                Properties props = new Properties();
                props.load(new StringReader(propString));
                for (String stringPropertyName : props.stringPropertyNames()) {
                    String value = props.getProperty(stringPropertyName);
                    map.put(
                            stringPropertyName,
                            enableSubstitution ? substitute(attributes, value) :
                                    value
                    );
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("Unable to parse properties", e);
            }
        }

        attributes.putAll(map);
    }

    /**
     * Substitutes any references in the value with the values from the attributes map.
     *
     * @param attributes attributes
     * @param value      value string
     * @return new string
     */
    private static String substitute(Map<String, String> attributes, String value) {
        Matcher matcher = PROP_REF_PATTERN.matcher(value);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group("name");
            String replacement = attributes.get(name);
            matcher.appendReplacement(sb, Objects.requireNonNullElse(replacement, ""));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static boolean matchesAll(final Map<String, String> attributes, String matches) {
        boolean regexMatch = true;
        Map<String, Predicate<String>> comparisons = new HashMap<>();
        for (final String s : matches.split("\r?\n")) {
            Matcher matcher = ComparisonPattern.matcher(s);
            if (matcher.matches()) {
                String key = matcher.group("key");
                String op = matcher.group("op");
                String val = matcher.group("val");
                Predicate<String> newOp = makePredicate(op, val);
                comparisons.compute(key, (s1, stringPredicate) ->
                        stringPredicate != null ? stringPredicate.and(newOp) : newOp
                );
            }else{
                regexMatch = false;
            }
        }
        if(!regexMatch){
            return false;
        }
        for (String s : comparisons.keySet()) {
            if (!comparisons.get(s).test(attributes.get(s))) {
                return false;
            }
        }
        return true;
    }

    static Predicate<String> makePredicate(final String op, final String val) {
        if ("==".equals(op)) {
            return val::equals;
        } else if ("!=".equals(op)) {
            return makePredicate("==", val).negate();
        } else if ("!!".equals(op)) {
            return Objects::isNull;
        } else if ("~~".equals(op)) {
            return Predicate.not(Objects::isNull);
        } else if ("=~".equals(op)) {
            Pattern p = Pattern.compile(val);
            return (s) -> s != null && p.matcher(s).matches();
        } else if ("!~".equals(op)) {
            return makePredicate("=~", val).negate();
        } else {
            throw new IllegalArgumentException();
        }
    }
}
