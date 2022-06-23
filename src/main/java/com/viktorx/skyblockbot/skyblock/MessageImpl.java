package com.viktorx.skyblockbot.skyblock;

import com.mojang.brigadier.Message;

public class MessageImpl implements Message {
    private final String message;

    public MessageImpl(String message) {
        this.message = message;
    }

    @Override
    public String getString() {
        return this.message;
    }
}
