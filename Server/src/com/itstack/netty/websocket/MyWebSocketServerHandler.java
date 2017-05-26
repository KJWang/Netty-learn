package com.itstack.netty.websocket;

import java.io.IOException;
import java.net.SocketAddress;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itstack.netty.common.ChannelInfo;
import com.itstack.netty.common.Global;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.StringUtil;

public class MyWebSocketServerHandler extends
		SimpleChannelInboundHandler<Object> {

	private static final Logger logger = Logger
			.getLogger(WebSocketServerHandshaker.class.getName());

	private WebSocketServerHandshaker handshaker;


	public static HashMap<String,ChannelInfo> listChannel = new HashMap<String,ChannelInfo>();

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		listChannel.put(ctx.channel().id().toString(),new ChannelInfo(ctx.channel()));
		// ���
		Global.group.add(ctx.channel());

		System.out.println("�ͻ������������ӿ���");
	}

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (IdleStateEvent.class.isAssignableFrom(evt.getClass())) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                System.out.println("read idle");
                ctx.channel().close();
            } else if (event.state() == IdleState.WRITER_IDLE) {
                System.out.println("write idle");
            } else if (event.state() == IdleState.ALL_IDLE) {
                System.out.println("all idle");
            }
        }
    }


    @Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {

	    listChannel.remove(ctx.channel().id().toString());
		// �Ƴ�
		Global.group.remove(ctx.channel());

		System.out.println("�ͻ������������ӹر�");

	}

	@Override
	protected void messageReceived(ChannelHandlerContext ctx, Object msg)
			throws Exception {

		if (msg instanceof FullHttpRequest) {

			handleHttpRequest(ctx, ((FullHttpRequest) msg));

		} else if (msg instanceof WebSocketFrame) {

			handlerWebSocketFrame(ctx, (WebSocketFrame) msg);

		}

	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	private void handlerWebSocketFrame(ChannelHandlerContext ctx,
			WebSocketFrame frame) {

		// �ж��Ƿ�ر���·��ָ��
		if (frame instanceof CloseWebSocketFrame) {
			handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame
					.retain());
		}

		// �ж��Ƿ�ping��Ϣ
		if (frame instanceof PingWebSocketFrame) {
            System.out.println("������յ���" + frame.toString());
            ctx.channel().write(
					new PongWebSocketFrame(frame.content().retain()));
			return;
		}

		// �����̽�֧���ı���Ϣ����֧�ֶ�������Ϣ
		if (!(frame instanceof TextWebSocketFrame)) {

			//System.out.println("�����̽�֧���ı���Ϣ����֧�ֶ�������Ϣ");
			return;
			//throw new UnsupportedOperationException(String.format(
					//"%s frame types not supported", frame.getClass().getName()));
		}
		// ����Ӧ����Ϣ
		String request = ((TextWebSocketFrame) frame).text();
		System.out.println("������յ���" + request);

		if (logger.isLoggable(Level.FINE)) {
			logger.fine(String.format("%s received %s", ctx.channel(), request));
		}

        Map<String, String> map = null;
		try {
            ObjectMapper mapper = new ObjectMapper();
            map = mapper.readValue(request, Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String resp = null;
        if (map != null) {
		    String type = map.get("type");
		    if ("name".equals(type)) {
		        String name = map.get("name");
		        if (name == null || name.trim().length() == 0) {
		            return;
                }
		        ChannelInfo info = listChannel.get(ctx.channel().id().toString());
		        if (info.getName() != null) {
		            if (info.getName().equals(name)) {
                        ctx.channel().writeAndFlush(new TextWebSocketFrame("���Ѿ�ע����"));
                        return;
		            } else {
                        resp = "\"" + info.getName() + "\" ����Ϊ \"" + name + "\"";
                    }
                } else {
                    resp = "\"" + name + "\" ������";
                }
		        info.setName(name);
            } else if ("msg".equals(type)) {
                ChannelInfo info = listChannel.get(ctx.channel().id().toString());
                if (info.getName() == null) {
                    ctx.channel().writeAndFlush(new TextWebSocketFrame("���������ֲ��ܷ���Ϣ"));
                    return;
                }
                resp = info.getName() + "��" + map.get("msg");
            }
            TextWebSocketFrame tws = new TextWebSocketFrame(resp);
            // Ⱥ��
            Global.group.writeAndFlush(tws);
        }

		// ���ء�˭���ķ���˭��
		//ctx.channel().writeAndFlush(tws);
	}

	public static String getStringDate() {
		Date currentTime = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("MM-dd HH:mm:ss");
		String dateString = formatter.format(currentTime);
		return dateString;
	}

	private void handleHttpRequest(ChannelHandlerContext ctx,
			FullHttpRequest req) {
	    for (Object object : req.headers()) {
            System.out.println(object);
        }

		if (!req.getDecoderResult().isSuccess()
				|| (!"websocket".equals(req.headers().get("Upgrade")))) {

			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(
					HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));

			return;
		}

		WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
				"ws://localhost:7397/websocket", null, false);

		handshaker = wsFactory.newHandshaker(req);

		if (handshaker == null) {
			WebSocketServerHandshakerFactory
					.sendUnsupportedWebSocketVersionResponse(ctx.channel());
		} else {
			handshaker.handshake(ctx.channel(), req);
		}

	}

	private static void sendHttpResponse(ChannelHandlerContext ctx,
			FullHttpRequest req, DefaultFullHttpResponse res) {

		// ����Ӧ����ͻ���
		if (res.getStatus().code() != 200) {
			ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(),
					CharsetUtil.UTF_8);
			res.content().writeBytes(buf);
			buf.release();
		}

		// ����Ƿ�Keep-Alive���ر�����
		ChannelFuture f = ctx.channel().writeAndFlush(res);
		if (!isKeepAlive(req) || res.getStatus().code() != 200) {
			f.addListener(ChannelFutureListener.CLOSE);
		}
	}

	private static boolean isKeepAlive(FullHttpRequest req) {

		return true;
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {

		cause.printStackTrace();
		ctx.close();

	}

}
