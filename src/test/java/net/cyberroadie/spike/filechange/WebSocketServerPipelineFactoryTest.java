package net.cyberroadie.spike.filechange;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Matchers.*;

/**
 * User: olivier
 * Date: 13/07/2012
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Channels.class)
public class WebSocketServerPipelineFactoryTest {

    @Mock
    ChannelPipeline channelPipelineMock;

    @Mock
    MathexRepository mathexRepository;

    @Test
    public void testGetPipeline() throws Exception {
        PowerMockito.mockStatic(Channels.class);
        Mockito.when(Channels.pipeline()).thenReturn(channelPipelineMock);
        (new WebSocketServerPipelineFactory(mathexRepository)).getPipeline();
        Mockito.verify(channelPipelineMock, Mockito.times(1)).addLast(eq("decoder"), isA(HttpRequestDecoder.class));
        Mockito.verify(channelPipelineMock, Mockito.times(1)).addLast(eq("aggregator"), isA(HttpChunkAggregator.class));
        Mockito.verify(channelPipelineMock, Mockito.times(1)).addLast(eq("encoder"), isA(HttpResponseEncoder.class));
        Mockito.verify(channelPipelineMock, Mockito.times(1)).addLast(eq("handler"), isA(WebSocketServerHandler.class));
    }
}
