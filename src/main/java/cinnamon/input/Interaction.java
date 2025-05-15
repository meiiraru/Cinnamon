package cinnamon.input;

import cinnamon.settings.Settings;
import cinnamon.vr.XrManager;
import cinnamon.world.entity.living.LivingEntity;
import cinnamon.world.entity.living.LocalPlayer;

public class Interaction {

    private boolean attacking, using;
    private boolean wasAttacking, wasUsing;

    private int scrollItem;

    public void tick(LivingEntity target) {
        if (!XrManager.isInXR()) {
            attacking = Settings.attack.get().isPressed();
            using = Settings.use.get().isPressed();

            if (target instanceof LocalPlayer lp && Settings.pick.get().click())
                lp.pick();
        }

        if (scrollItem != 0) {
            target.setSelectedItem(target.getInventory().getSelectedIndex() + scrollItem);
            scrollItem = 0;
        }

        if (attacking) {
            target.attackAction();
            wasAttacking = true;
        } else if (wasAttacking) {
            target.stopAttacking();
            wasAttacking = false;
        }

        if (using) {
            target.useAction();
            wasUsing = true;
        } else if (wasUsing) {
            target.stopUsing();
            wasUsing = false;
        }
    }

    public void reset() {
        attacking = using = wasAttacking = wasUsing = false;
    }

    public void scrollItem(int amount) {
        scrollItem = amount;
    }

    public void xrTriggerPress(int button, float value, int hand, float lastValue) {
        boolean left = hand == 0;
        boolean pressed = value > 0.5f;

        if (button == 0) {
            if (left) using = pressed;
            else attacking = pressed;
        }

        else if (button == 1 && pressed && lastValue < 0.5f) {
            scrollItem(left ? -1 : 1);
        }
    }
}
