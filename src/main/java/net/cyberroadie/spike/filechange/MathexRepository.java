package net.cyberroadie.spike.filechange;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private File file;
    private String mathtexService = "http://www.cyberroadie.org/cgi-bin/mathtex.cgi?";
    private RandomAccessFile fileHandler;

    public MathexRepository(String pathToWatch, String fileToParse) throws IOException {
        this.pathToWatch = pathToWatch;
        this.file = new File(pathToWatch + fileToParse);
        this.fileHandler = new RandomAccessFile(file, "r");
        setPositionToEndOfFile();
    }

    public void setPositionToEndOfFile() throws IOException {
        fileHandler.seek(file.length() - 1);
    }

    public String readLineBackwards() throws IOException {
        StringBuilder input = new StringBuilder();
        int c = -1;
        int eol = 0;
        long currentPosition = fileHandler.getFilePointer();

        while (eol != 1 && currentPosition > 0) {
            switch (c = fileHandler.read()) {
                case -1:
                case '\n':
                    eol++;
                    break;
                default:
                    fileHandler.seek(--currentPosition);
                    input.append((char)c);
                    break;
            }
        }

        if ((currentPosition <= 0) && (input.length() == 0)) {
            return null;
        }
        if(currentPosition > 0)fileHandler.seek(--currentPosition);
        return input.reverse().toString();
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

            for (; ; ) {
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

    public String getLast() throws IOException {
        return mathtexService + readLineBackwards();
    }

    public void sendLast() {
        try {
            for (ChannelHandlerContext channelContext : channelContexts) {
                channelContext.getChannel().write(new TextWebSocketFrame(getLast()));
            }
        } catch (IOException ex) {

        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void addCtx(ChannelHandlerContext ctx) {
        channelContexts.add(ctx);
    }

    public List<String> readRecord() {
        List<String> lines = new ArrayList<>();
        try {
            String line = readLineBackwards();
            lines.add(line);

            line = readLineBackwards();
            while(line != null && !line.endsWith("-----------"))  {
                lines.add(line);
                line = readLineBackwards();
            }

        } catch (IOException e) {
            return null;
        }
        Collections.reverse(lines);
        return lines;
    }
}
