package com.itstack.netty.websocket;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

public class ChildChannelHandler extends ChannelInitializer<SocketChannel>{

	@Override
	protected void initChannel(SocketChannel e) throws Exception {
		
		e.pipeline().addLast("http-codec",new HttpServerCodec());
		e.pipeline().addLast("aggregator",new HttpObjectAggregator(65536));
		e.pipeline().addLast("http-chunked",new ChunkedWriteHandler());
		e.pipeline().addLast("idleStateHandler", new IdleStateHandler(6, 0, 0, TimeUnit.SECONDS));
        e.pipeline().addLast("handler",new MyWebSocketServerHandler());
	}

}
