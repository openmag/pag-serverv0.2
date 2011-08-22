package com.anheinno.pag.server.handler;

import java.util.concurrent.TimeUnit;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.timeout.ReadTimeoutHandler;

import com.anheinno.pag.lib.codec.PagMessageDecoder;
import com.anheinno.pag.lib.codec.PagMessageEncoder;
import com.anheinno.pag.lib.util.TimerUtil;

public class PagServerPipelineFactory implements ChannelPipelineFactory {
	
	public ChannelPipeline getPipeline() throws Exception {

        ChannelPipeline pipeline = Channels.pipeline();

        // Uncomment the following line if you want HTTPS
        //SSLEngine engine = SecureChatSslContextFactory.getServerContext().createSSLEngine();
        //engine.setUseClientMode(false);
        //pipeline.addLast("ssl", new SslHandler(engine));
        
        // disable read timeout
        pipeline.addLast("readtimeout", new ReadTimeoutHandler(TimerUtil.getTimer(), 7, TimeUnit.DAYS));

        pipeline.addLast("decoder", new PagMessageDecoder());

        pipeline.addLast("encoder", new PagMessageEncoder());
        
        pipeline.addLast("handler", new PagServerHandler());
        
        return pipeline;
    }
	
}
