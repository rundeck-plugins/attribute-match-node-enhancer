package org.rundeck.plugins.nodes.attributes;

import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.descriptions.*;
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
                            + "* `!!` not present match\n"
                            + "* `=~` regular expression match\n"
                            + "* `!=` inequality match\n"
                            + "* `!~` negative regular expression match\n",
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


    Map<String, String> loadedProps;
    Set<String> loadedTags;
    static final Pattern ComparisonPattern = Pattern.compile(
            "^(?<key>.+?)(?<op>==|!=|=~|!~|!!)(?<val>.*)$"
    );

    @Override
    public void updateNode(
            final String project, final IModifiableNodeEntry node
    )
    {
        if (!matchesAll(node.getAttributes())) {
            return;
        }
        addAll(node.getAttributes());
        addAllTags(node.getTags());
    }

    private void addAllTags(final Set tags) {
        if (null == loadedTags) {
            loadedTags = new HashSet<>();
            if (addTags != null && !"".equals(addTags.trim())) {
                loadedTags.addAll(Arrays.asList(addTags.split(",\\s*")));
            }
        }
        tags.addAll(loadedTags);
    }


    private void addAll(final Map<String, String> attributes) {
        if (null == loadedProps) {
            Map<String, String> map = new HashMap<>();
            if (add != null && !"".equals(add.trim())) {
                try {
                    Properties props = new Properties();
                    props.load(new StringReader(add));
                    for (String stringPropertyName : props.stringPropertyNames()) {
                        map.put(stringPropertyName, props.getProperty(stringPropertyName));
                    }
                } catch (IOException e) {
                    throw new IllegalArgumentException();
                }
            }
            loadedProps = map;
        }
        attributes.putAll(loadedProps);
    }

    private boolean matchesAll(final Map<String, String> attributes) {
        boolean regexMatch = true;
        Map<String, Predicate<String>> comparisons = new HashMap<>();
        for (final String s : match.split("\r?\n")) {
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

    private Predicate<String> makePredicate(final String op, final String val) {
        if ("==".equals(op)) {
            return val::equals;
        } else if ("!=".equals(op)) {
            return makePredicate("==", val).negate();
        } else if ("!!".equals(op)) {
            return Objects::isNull;
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
