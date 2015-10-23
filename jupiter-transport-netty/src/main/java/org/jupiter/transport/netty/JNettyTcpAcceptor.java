package org.jupiter.transport.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.jupiter.rpc.provider.processor.DefaultProviderProcessor;
import org.jupiter.transport.JConfig;
import org.jupiter.transport.JOption;
import org.jupiter.transport.netty.handler.IdleStateChecker;
import org.jupiter.transport.netty.handler.ProtocolDecoder;
import org.jupiter.transport.netty.handler.ProtocolEncoder;
import org.jupiter.transport.netty.handler.acceptor.AcceptorHandler;
import org.jupiter.transport.netty.handler.acceptor.AcceptorIdleStateTrigger;

import java.net.SocketAddress;

import static org.jupiter.common.util.JConstants.READER_IDLE_TIME_SECONDS;

/**
 * jupiter
 * org.jupiter.transport.netty
 *
 * @author jiachun.fjc
 */
public class JNettyTcpAcceptor extends NettyTcpAcceptor {

    // handlers
    private final AcceptorIdleStateTrigger idleStateTrigger = new AcceptorIdleStateTrigger();
    private final AcceptorHandler handler = new AcceptorHandler(new DefaultProviderProcessor(this));
    private final ProtocolEncoder encoder = new ProtocolEncoder();

    public JNettyTcpAcceptor(int port) {
        super(port);
    }

    public JNettyTcpAcceptor(SocketAddress address) {
        super(address);
    }

    public JNettyTcpAcceptor(int port, int nWorks) {
        super(port, nWorks);
    }

    public JNettyTcpAcceptor(SocketAddress address, int nWorks) {
        super(address, nWorks);
    }

    public JNettyTcpAcceptor(int port, boolean nativeEt) {
        super(port, nativeEt);
    }

    public JNettyTcpAcceptor(SocketAddress address, boolean nativeEt) {
        super(address, nativeEt);
    }

    public JNettyTcpAcceptor(int port, int nWorks, boolean nativeEt) {
        super(port, nWorks, nativeEt);
    }

    public JNettyTcpAcceptor(SocketAddress address, int nWorks, boolean nativeEt) {
        super(address, nWorks, nativeEt);
    }

    @Override
    protected void init() {
        super.init();

        // parent options
        JConfig parent = configGroup().parent();
        parent.setOption(JOption.SO_BACKLOG, 32768);
        parent.setOption(JOption.SO_REUSEADDR, true);

        // child options
        JConfig child = configGroup().child();
        child.setOption(JOption.SO_REUSEADDR, true);
    }

    @Override
    public ChannelFuture bind(SocketAddress address) {
        ServerBootstrap boot = bootstrap();

        if (isNativeEt()) {
            boot.channel(EpollServerSocketChannel.class);
        } else {
            boot.channel(NioServerSocketChannel.class);
        }
        boot.childHandler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(
                        new IdleStateChecker(timer, READER_IDLE_TIME_SECONDS, 0, 0),
                        idleStateTrigger,
                        new ProtocolDecoder(),
                        encoder,
                        handler);
            }
        });

        setOptions();

        return boot.bind(address);
    }
}
