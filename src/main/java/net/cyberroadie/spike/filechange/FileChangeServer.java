package net.cyberroadie.spike.filechange;

import java.io.FileNotFoundException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

public class FileChangeServer {

    private final int port;

    public FileChangeServer(int port) {
        this.port = port;
    }

    public void run() {
        ServerBootstrap bootstrap =
                new ServerBootstrap(
                        new NioServerSocketChannelFactory(
                        Executors.newCachedThreadPool(), Executors.newCachedThreadPool())
                );
        try {
            bootstrap.setPipelineFactory(new WebSocketServerPipelineFactory());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
        bootstrap.bind(new InetSocketAddress(port));
        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);
    }

    public static void main(String[] args) {
        int port;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        } else {
            port = 8080;
        }
        new FileChangeServer(port).run();
    }
}