package net.cyberroadie.spike.filechange;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

public class FileChangeServer {

    private final int port;
    private String directory;
    private String file;

    public FileChangeServer(int port, String directory, String file) {
        this.port = port;
        this.file = file;
        this.directory = directory;
    }

    public void run() {
        ServerBootstrap bootstrap =
                new ServerBootstrap(
                        new NioServerSocketChannelFactory(
                        Executors.newCachedThreadPool(), Executors.newCachedThreadPool())
                );
        try {
            bootstrap.setPipelineFactory(new WebSocketServerPipelineFactory(new MathexRepository(directory, file)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        bootstrap.bind(new InetSocketAddress(port));
        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);
    }

    public static void main(String[] args) {
        int port;
        String directory = null;
        String file;

        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
            directory = args[1];
            file = args[2];
            new FileChangeServer(port, directory, file).run();
        } else {
            System.out.println("Usage java -jar FileChangeServer.jar [port] [directory to watch] [file to parse]");
        }

    }
}