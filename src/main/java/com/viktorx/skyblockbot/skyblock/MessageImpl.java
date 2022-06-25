package com.viktorx.skyblockbot.skyblock;

import com.mojang.brigadier.Message;

public record MessageImpl(String message) implements Message {

    @Override
    public String getString() {
        return this.message;
    }
}
