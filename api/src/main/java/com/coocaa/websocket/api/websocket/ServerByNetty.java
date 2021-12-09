package com.coocaa.websocket.api.websocket;

import com.coocaa.websocket.api.websocket.handle.AuthHandler;
import com.coocaa.websocket.api.websocket.handle.BusinessChannelHandle;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;

/**
 * websocket的服务端代码 目前不支持ssl
 *
 * @Auth liangshizhu
 */
@Configuration
public class ServerByNetty {
    @Value("${miniProgram.websocket.port}")
    Integer port;
    public static final String WEBSOCKET_PATH = "/ws";

    /**
     * 服务端启动类
     *
     * @throws Exception
     */
    public void startServer() throws Exception {
        // netty基本操作，两个线程组，一个负责连接，一个负责io
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup wokerGroup = new NioEventLoopGroup();
        try {
            //netty的启动类
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            //池子内指定channel类型
            serverBootstrap.group(bossGroup, wokerGroup).channel(NioServerSocketChannel.class)
                    //记录日志的handler，netty自带的
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.SO_BACKLOG, 1024 * 1024 * 10)
                    //设置handler
                    //没有key的handle会自动生成key
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            pipeline.addLast(new HttpServerCodec());
                            pipeline.addLast(new HttpObjectAggregator(65536));
                            pipeline.addLast(new WebSocketServerCompressionHandler());
                            pipeline.addLast(new AuthHandler());
                            pipeline.addLast(new WebSocketServerProtocolHandler(WEBSOCKET_PATH, null, true));
                            pipeline.addLast(new BusinessChannelHandle());
                        }
                    });
            ChannelFuture channelFuture = serverBootstrap.bind(new InetSocketAddress(port)).sync();
            channelFuture.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            wokerGroup.shutdownGracefully();
        }

    }
}
