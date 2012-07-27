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
    private File file;
    private String mathtexService = "http://www.cyberroadie.org/cgi-bin/mathtex.cgi?";

    public MathexRepository(String pathToWatch, String fileToParse) throws FileNotFoundException {
        this.pathToWatch = pathToWatch;
        this.file = new File(pathToWatch + fileToParse);
    }

    public String tail(int lines) {
        try {
            RandomAccessFile fileHandler = new RandomAccessFile( file, "r" );
            long fileLength = file.length() - 1;
            StringBuilder sb = new StringBuilder();
            int line = 0, delimiter = 0;
            List<String> lastLines = new ArrayList<>();

            for( long filePointer = fileLength; filePointer != -1; filePointer-- ) {
                fileHandler.seek( filePointer ); // end of file
                int c = fileHandler.read();

                if( c == '\r' ) {
                    if (line == lines) {
                        if (filePointer == fileLength) {
                            continue;
                        } else {
                            lastLines.add(sb.reverse().toString());
                            break;
                        }
                    }
                } else if( c == '\n' ) {
                    line = line + 1;
                    if (line == lines) {
                        if (filePointer == fileLength - 1) {
                            continue;
                        } else {
                            lastLines.add(sb.reverse().toString());
                            break;
                        }
                    }
                    String reverse = sb.reverse().toString();
                    if(reverse.endsWith("--------------\n")) {
                        delimiter++;
                    } else if (!reverse.endsWith(".gif\n") && !reverse.startsWith("http://") && !reverse.equals("")) {
                        lastLines.add(reverse);
                    }
                    sb.delete(0, sb.length());
                    if(delimiter == 2) break;
                }
                sb.append( ( char ) c );
            }

//            sb.deleteCharAt(sb.length() - 1);

            return lastLines.get(lastLines.size() - 1);
        } catch( java.io.FileNotFoundException e ) {
            return null;
        } catch( java.io.IOException e ) {
            return null;
        }
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
        return mathtexService + tail(8);
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
}
