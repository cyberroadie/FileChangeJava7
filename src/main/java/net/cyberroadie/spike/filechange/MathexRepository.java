package net.cyberroadie.spike.filechange;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * User: olivier
 * Date: 14/07/2012
 */
public class MathexRepository implements Runnable {

    Logger logger = Logger.getLogger(MathexRepository.class.getName());

    private boolean isRunning = false;
    private List<ChannelHandlerContext> channelContexts = new ArrayList<>();
    private String pathToWatch;
    private BufferedReader input;

    public MathexRepository(String pathToWatch, String fileToParse) throws FileNotFoundException {
        this.pathToWatch = pathToWatch;
        this.input = new BufferedReader(new FileReader(pathToWatch + fileToParse));;
    }

    public List<String> tail(int maxLines) throws IOException {
        String[] lines = new String[maxLines];
        int lastNdx = 0;
        for (String line = input.readLine(); line != null; line = input.readLine()) {
            if (lastNdx == lines.length) {
                lastNdx = 0;
            }
            lines[lastNdx++] = line;
        }

        List<String> mathexList = new ArrayList<>();

        for (int ndx = lastNdx; ndx != lastNdx - 1; ndx++) {
            if (ndx == lines.length) {
                ndx = 0;
            }
            mathexList.add(lines[ndx]);
        }
        return mathexList;
    }

    @Override
    public void run() {
        isRunning = true;
        Path dir = Paths.get(pathToWatch);

        try {
            WatchService watchService = dir.getFileSystem().newWatchService();
            WatchKey key = dir.register(watchService,
                    ENTRY_CREATE,
                    ENTRY_DELETE,
                    ENTRY_MODIFY);

            for (;;) {
                key = watchService.take();
                if (key != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.kind() == ENTRY_CREATE || event.kind() == ENTRY_DELETE || event.kind() == ENTRY_MODIFY) {
                            for (ChannelHandlerContext channelContext : channelContexts) {
                                channelContext.getChannel().write(new TextWebSocketFrame(getLast()));
                            }
                        }
                    }
                }
                key.reset();
            }
        } catch (Exception ex) {
            isRunning = false;
            logger.severe("Error: " + ex.toString());
            ex.printStackTrace();
        }

    }

    public String getLast() {
        return "http://www.cyberroadie.org/cgi-bin/mathtex.cgi?\\sigma%20=%20{1%20\\over%20{2\\pi%20}}\\sqrt%20{{K%20\\over%20\\mu%20}}";
    }

    public void sendLast() {
        for (ChannelHandlerContext channelContext : channelContexts) {
            channelContext.getChannel().write(new TextWebSocketFrame(getLast()));
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public static void main(String[] args) throws FileNotFoundException {
        ExecutorService threadExecutor = Executors.newSingleThreadExecutor();
        MathexRepository mathexRepository = new MathexRepository("/Users/olivier/tmp", "mathex.log");
        threadExecutor.execute(mathexRepository);
    }

    public void addCtx(ChannelHandlerContext ctx) {
        channelContexts.add(ctx);
    }
}
