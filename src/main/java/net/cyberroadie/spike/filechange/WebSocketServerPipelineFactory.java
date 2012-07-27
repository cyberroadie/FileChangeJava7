package net.cyberroadie.spike.filechange;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;

import java.io.FileNotFoundException;

import static org.jboss.netty.channel.Channels.pipeline;

/**
 * User: olivier
 * Date: 13/07/2012
 */
public class WebSocketServerPipelineFactory implements ChannelPipelineFactory {

    MathexRepository mathexRepository;

    public WebSocketServerPipelineFactory(MathexRepository mathexRepository) throws FileNotFoundException {
        this.mathexRepository = mathexRepository;
    }

    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = pipeline();
        pipeline.addLast("decoder", new HttpRequestDecoder());
        pipeline.addLast("aggregator", new HttpChunkAggregator(65536));
        pipeline.addLast("encoder", new HttpResponseEncoder());
        pipeline.addLast("handler", new WebSocketServerHandler(mathexRepository));
        return pipeline;
    }
}
