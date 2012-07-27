package net.cyberroadie.spike.filechange;

import static org.jboss.netty.handler.codec.http.HttpHeaders.*;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.*;
import static org.jboss.netty.handler.codec.http.HttpMethod.*;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.*;
import static org.jboss.netty.handler.codec.http.HttpVersion.*;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import org.jboss.netty.logging.InternalLogger;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.util.CharsetUtil;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles handshakes and messages
 */
public class WebSocketServerHandler extends SimpleChannelUpstreamHandler {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(WebSocketServerHandler.class);

    private static final String WEBSOCKET_PATH = "/websocket";
    private WebSocketServerHandshaker handshaker = null;
    private MathexRepository mathexRepository = null;
    private ExecutorService threadExecutor = Executors.newSingleThreadExecutor();

    public WebSocketServerHandler(MathexRepository mathexRepository) {
        this.mathexRepository = mathexRepository;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        Object msg = e.getMessage();
        if (msg instanceof HttpRequest) {
            handleHttpRequest(ctx, (HttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    public void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest req) throws Exception {
        if (("websocket").equals(req.getHeader("Upgrade"))) {
            upgradeToWebsocket(ctx, req);
        } else {
            if (req.getMethod() == GET) {
                HttpResponse res;
                switch (req.getUri()) {
                    case "/":
                        res = createResponseWithContent(req);
                        break;
                    default:
                        res = new DefaultHttpResponse(HTTP_1_1, NOT_FOUND);
                }
                sendHttpResponse(ctx, req, res);
            } else {
                sendHttpResponse(ctx, req, new DefaultHttpResponse(HTTP_1_1, FORBIDDEN));
            }
        }
    }

    private HttpResponse createResponseWithContent(HttpRequest req) throws Exception {
        HttpResponse res;
        res = new DefaultHttpResponse(HTTP_1_1, OK);
        ChannelBuffer content = ChannelBuffers.copiedBuffer(processTemplate("imageviewer.vm", "Image Viewer"), CharsetUtil.UTF_8);
        res.setHeader(CONTENT_TYPE, "text/html; charset=UTF-8");
        setContentLength(res, content.readableBytes());
        res.setContent(content);
        return res;
    }

    public String processTemplate(String templateName, String title) throws IOException {
        Properties p = new Properties();
        p.setProperty("resource.loader", "class");
        p.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init(p);

        VelocityContext context = new VelocityContext();
        context.put("title", title);
        StringWriter writer = new StringWriter();
        Velocity.mergeTemplate(templateName, "UTF-8", context, writer);
        return writer.toString();
    }


    private void upgradeToWebsocket(ChannelHandlerContext ctx, HttpRequest req) {
        WebSocketServerHandshakerFactory wsFactory =
                new WebSocketServerHandshakerFactory("ws://" + req.getHeader(Names.HOST) + WEBSOCKET_PATH, null, false);
        handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            wsFactory.sendUnsupportedWebSocketVersionResponse(ctx.getChannel());
        } else {
            handshaker.handshake(ctx.getChannel(), req).addListener(WebSocketServerHandshaker.HANDSHAKE_LISTENER);
        }
        mathexRepository.addCtx(ctx);
        if (!mathexRepository.isRunning()) {
            threadExecutor.execute(mathexRepository);
        }
    }

    public void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (frame instanceof TextWebSocketFrame) {
            mathexRepository.sendLast();
        } else if (frame instanceof CloseWebSocketFrame) {
            if(handshaker != null) handshaker.close(ctx.getChannel(), (CloseWebSocketFrame) frame);
        } else if (frame instanceof PingWebSocketFrame) {
            ctx.getChannel().write(new PongWebSocketFrame(frame.getBinaryData()));
        } else {
            throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass().getName()));
        }
    }

    private static void sendHttpResponse(ChannelHandlerContext ctx, HttpRequest req, HttpResponse res) {
        if (res.getStatus().getCode() != 200) {
            res.setContent(ChannelBuffers.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8));
            setContentLength(res, res.getContent().readableBytes());
        }
        ChannelFuture f = ctx.getChannel().write(res);
        if (!isKeepAlive(req) || res.getStatus().getCode() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        e.getCause().printStackTrace();
        e.getChannel().close();
    }

    private static String getWebSocketLocation(HttpRequest req) {
        return "ws://" + req.getHeader(HttpHeaders.Names.HOST) + WEBSOCKET_PATH;
    }

    public MathexRepository getMathexRepository() {
        return mathexRepository;
    }

    public void setMathexRepository(MathexRepository mathexRepository) {
        this.mathexRepository = mathexRepository;
    }
}