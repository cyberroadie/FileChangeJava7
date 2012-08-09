package net.cyberroadie.spike.filechange;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * User: olivier
 * Date: 14/07/2012
 */
@RunWith(MockitoJUnitRunner.class)
public class WebSocketServerHandlerTest {

    @Mock
    public MathexRepository mathexRepository;

    @Mock
    public ChannelHandlerContext ctx;

    @Mock
    public MessageEvent mEvent;

    @Mock
    private TextWebSocketFrame textWebSocketFrame;

    @Mock
    private CloseWebSocketFrame closeWebSocketFrame;

    @Test
    public void testHandleHttpRequest() throws Exception {

    }

    @Test
    public void testHandleWebSocketFrameWithTextFrame() throws Exception {
        WebSocketServerHandler classToTest = new WebSocketServerHandler(mathexRepository);
        classToTest.handleWebSocketFrame(ctx, textWebSocketFrame);
        verify(mathexRepository, times(1)).sendLast();
    }

    @Test
    public void testHandleWebSocketFrameWithCloseWebSocketFrame() throws Exception {
        WebSocketServerHandler classToTest = new WebSocketServerHandler(mathexRepository);
        classToTest.handleWebSocketFrame(ctx, closeWebSocketFrame);
        verify(mathexRepository, times(0)).sendLast();
    }

    @Test
    public void testMessageReceived() throws Exception {

//        classToTest.messageReceived(ctx, mEvent);
          assertTrue(true);

    }
}

