package cinnamon.world;

import java.util.HashMap;

public class WorldRules {

    private final HashMap<Rule, Object> rules = new HashMap<>();

    public WorldRules() {
        for (Rule rule : Rule.values())
            rules.put(rule, rule.initialState);
    }

    public WorldRules set(Rule rule, Object value) {
        rules.put(rule, value);
        return this;
    }

    public Object get(Rule rule) {
        return rules.getOrDefault(rule, rule.initialState);
    }

    public enum Rule {
        DAY_CYCLE(Type.BOOL, true);

        public final Type type;
        public final Object initialState;

        Rule(Type type, Object initialState) {
            this.type = type;
            this.initialState = initialState;
        }
    }

    public enum Type {
        BOOL, INT, FLOAT, STRING;
    }
}

