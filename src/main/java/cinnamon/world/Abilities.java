package cinnamon.world;

import java.util.HashMap;

public class Abilities {

    private final HashMap<Ability, Boolean> abilities = new HashMap<>();

    public Abilities() {
        for (Ability ability : Ability.values())
            abilities.put(ability, ability.initialState);
    }

    public Abilities set(Ability ability, boolean value) {
        abilities.put(ability, value);
        return this;
    }

    public boolean get(Ability ability) {
        return abilities.getOrDefault(ability, ability.initialState);
    }

    public enum Ability {
        GOD_MODE(false),
        CAN_FLY(false),
        NOCLIP(false),
        CAN_BUILD(true);

        public final boolean initialState;

        Ability(boolean initialState) {
            this.initialState = initialState;
        }
    }
}
