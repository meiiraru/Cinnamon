package cinnamon.messages;

import cinnamon.text.Text;
import cinnamon.world.entity.Entity;

public record Message(long addedTime, Text text, MessageCategory category, Entity source) {}
