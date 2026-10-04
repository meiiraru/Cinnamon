package cinnamon.world;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A class that manages world rules, allowing for the registration and retrieval of various rules that can be applied to a world
 */
public class WorldRules {

    // -- global registry -- //

    private static final Map<String, Rule<?>> REGISTRY = new HashMap<>();

    public static final Rule<Boolean>
            DAY_CYCLE         = registerBool("day_cycle",         true),
            ENABLE_TERMINAL   = registerBool("enable_terminal",   true),
            TERRAIN_EXPLOSION = registerBool("terrain_explosion", true);

    /**
     * Registers a new {@link Rule} with the given name, initial state, and parser function<br>
     * The rule is stored in a global registry for later retrieval
     * @param name The name of the rule
     * @param initialState The initial state of the rule
     * @param parser A function that takes a {@code String} and returns a value of type {@code T}, used to parse the rule value from a string representation
     * @return The newly registered rule
     */
    public static <T> Rule<T> register(String name, T initialState, Function<String, T> parser) {
        Rule<T> rule = new Rule<>(name.toLowerCase(), initialState, parser);
        REGISTRY.put(rule.getName(), rule);
        return rule;
    }

    /**
     * Shorthand register for {@code boolean} type {@link Rule}
     * @param name The name of the rule
     * @param initialState The initial state of the rule
     * @return The newly registered rule
     * @see #register(String, Object, Function)
     */
    public static Rule<Boolean> registerBool(String name, boolean initialState) {
        return register(name, initialState, Boolean::parseBoolean);
    }

    /**
     * Shorthand register for {@code int} type {@link Rule}
     * @param name The name of the rule
     * @param initialState The initial state of the rule
     * @return The newly registered rule
     * @see #register(String, Object, Function)
     */
    public static Rule<Integer> registerInt(String name, int initialState) {
        return register(name, initialState, Integer::parseInt);
    }

    /**
     * Shorthand register for {@code float} type {@link Rule}
     * @param name The name of the rule
     * @param initialState The initial state of the rule
     * @return The newly registered rule
     * @see #register(String, Object, Function)
     */
    public static Rule<Float> registerFloat(String name, float initialState) {
        return register(name, initialState, Float::parseFloat);
    }

    /**
     * Shorthand register for {@code String} type {@link Rule}
     * @param name The name of the rule
     * @param initialState The initial state of the rule
     * @return The newly registered rule
     * @see #register(String, Object, Function)
     */
    public static Rule<String> registerString(String name, String initialState) {
        return register(name, initialState, Function.identity());
    }

    /**
     * Gets a collection containing all registered {@link Rule}
     * @return A {@link java.util.Collection} of all registered {@link Rule}
     */
    public static Collection<Rule<?>> getRegisteredRules() {
        return REGISTRY.values();
    }

    /**
     * Gets a registered {@link Rule} by its name
     * @param name The name of the rule to retrieve
     * @return The {@link Rule} with the given name, or {@code null} if no such rule exists
     */
    public static Rule<?> getRule(String name) {
        return REGISTRY.get(name.toLowerCase());
    }


    // -- per world rules -- //


    private final HashMap<Rule<?>, Object> values = new HashMap<>();

    /**
     * Gets the value of a given {@link Rule} for this world, returning the rule initial state if the rule has not been set
     * @param rule The {@link Rule} to retrieve the value for
     * @return The value of the rule for this world
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Rule<T> rule) {
        return (T) values.getOrDefault(rule, rule.getInitialState());
    }

    /**
     * Sets the value of a given {@link Rule} for this world directly
     * @param rule The {@link Rule} to set the value for
     * @param value The value to set for the rule
     * @return This {@link WorldRules} instance, allowing for method chaining
     * @see #parseAndSet(Rule, String)
     */
    public <T> WorldRules set(Rule<T> rule, T value) {
        values.put(rule, value);
        return this;
    }

    /**
     * Parses a string value and sets the value of a given {@link Rule} for this world<br>
     * This method uses the rule own parser function to convert the string value into the appropriate type
     * @param rule The {@link Rule} to set the value for
     * @param value The {@code String} value to parse and set for the rule
     * @return This {@link WorldRules} instance, allowing for method chaining
     */
    public <T> WorldRules parseAndSet(Rule<T> rule, String value) {
        T parsedValue = rule.parse(value);
        set(rule, parsedValue);
        return this;
    }


    // -- the rule object -- //


    /**
     * An object representing a world rule
     */
    public static class Rule<T> {
        private final String name;
        private final T initialState;
        private final Function<String, T> parser;

        public Rule(String name, T initialState, Function<String, T> parser) {
            this.name = name;
            this.initialState = initialState;
            this.parser = parser;
        }

        /**
         * Gets the name of this rule
         * @return The name of the rule
         */
        public String getName() {
            return name;
        }

        /**
         * Gets the initial state of this rule
         * @return The initial state of the rule
         */
        public T getInitialState() {
            return initialState;
        }

        /**
         * Parses a string value into the appropriate type for this rule using the rule own parser function
         * @param value The {@code String} value to parse
         * @return The parsed value of type {@code T}
         */
        public T parse(String value) {
            return parser.apply(value);
        }
    }
}

