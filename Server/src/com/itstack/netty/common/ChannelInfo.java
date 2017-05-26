package com.itstack.netty.common;

import io.netty.channel.Channel;

/**
 * Created by wang on 2017/5/24.
 */
public class ChannelInfo {

    private String name;
    private Channel channel;
    private String id;

    public ChannelInfo(Channel channel) {
        setChannel(channel);
    }

    public String getName() {
        return name;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setChannel(Channel channel) {
        if (id != null) {
            id = channel.id().toString();
        }
        this.channel = channel;
    }

    public String getId() {
        return id;
    }
}
