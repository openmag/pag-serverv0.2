package com.anheinno.pag.server.handler;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import com.anheinno.pag.lib.codec.PagMessage;
import com.anheinno.pag.lib.codec.PagMethod;
import com.anheinno.pag.lib.codec.PagRequest;
import com.anheinno.pag.lib.codec.PagResponse;
import com.anheinno.pag.lib.codec.PagResponseStatus;
import com.anheinno.pag.lib.codec.PagUnknownMessage;
import com.anheinno.pag.lib.codec.PagVersion;
import com.anheinno.pag.lib.endpoint.PagApplicationEndpoint;
import com.anheinno.pag.lib.endpoint.PagClientEndpoint;
import com.anheinno.pag.lib.endpoint.PagEndpointManager;
import com.anheinno.pag.lib.endpoint.PagMobileEndpoint;

public class PagServerHandler extends SimpleChannelUpstreamHandler {
	
	public PagServerHandler() {
		super();
	}
	
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
    	PagClientEndpoint ep = PagEndpointManager.getInstance().getEndpointByChannel(e.getChannel());
		
    	System.out.println("Receive message: " + e.getMessage());
    	
    	if(ep == null) {
			
			System.out.println("Channel " + e.getChannel().getId() + " not exists!");
			
			// no connection
			PagMessage msg = (PagMessage)e.getMessage();
	    	
	    	if(msg == PagUnknownMessage.UNKNOWN) {
	    		sendError(msg, PagResponseStatus.NOT_ACCEPTABLE, e.getChannel(), true);
	    		return;
	    	}
	    	
	    	PagVersion version = msg.getProtocolVersion();
	    	
	    	if(version.equals(PagVersion.PAM_1_0) || version.equals(PagVersion.PAA_1_0)) {
	    		if(msg instanceof PagRequest && ((PagRequest)msg).getMethod().equals(PagMethod.REG)) {
					String id = ((PagRequest)msg).getDestination();
					
					ep = PagEndpointManager.getInstance().getEndpointById(id);
					if(ep == null) {
						if(version.equals(PagVersion.PAM_1_0)) {
	    					ep = new PagMobileEndpoint(id, e.getChannel());
	    				}else {
	    					ep = new PagApplicationEndpoint(id, e.getChannel());
	    				}
						PagEndpointManager.getInstance().addEndpoint(ep);	
					}else {
						PagEndpointManager.getInstance().replaceEndpointChannel(ep, e.getChannel());
					}
					ep.handleRequestMessage((PagRequest)msg);
					
				}else {
					sendError(msg, PagResponseStatus.METHOD_NOT_ALLOWED, e.getChannel(), true);
				}
	    	}else {
	    		sendError(msg, PagResponseStatus.PAG_VERSION_NOT_SUPPORTED, e.getChannel(), true);
	    	}
			
		}else {
			// connection exists!
			PagMessage msg = (PagMessage)e.getMessage();
	    	
	    	if(msg == PagUnknownMessage.UNKNOWN) {
	    		sendError(msg, PagResponseStatus.NOT_ACCEPTABLE, e.getChannel(), false);
	    		return;
	    	}
	    	
	    	PagVersion version = msg.getProtocolVersion();
			
			if(!version.equals(ep.getProtocolVersion())) {
				sendError(msg, PagResponseStatus.VERSION_MISMATCH, e.getChannel(), false);
			}else {
    			if(msg instanceof PagRequest){	    			
	    			ep.handleRequestMessage((PagRequest)msg);
	    		}else if(msg instanceof PagResponse) {
	    			ep.handleResponseMessage((PagResponse)msg);
	    		}
			}
		}
		    	
    }
    
    private void sendError(PagMessage origin, PagResponseStatus status, Channel ch, boolean close) {
    	PagVersion version = origin.getProtocolVersion();
    	PagVersion resp_version = null;
    	if(version.getProtocolName().equals("PAM")) {
    		resp_version = PagVersion.PAM_1_0;
    	}else if(version.getProtocolName().equals("PAA")) {
    		resp_version = PagVersion.PAA_1_0;
    	}else {
    		resp_version = PagVersion.PAM_1_0;
    	}
    	PagResponse resp = new PagResponse(resp_version, status);
    	ChannelFuture future = ch.write(resp);
    	if(close) {
    		future.addListener(ChannelFutureListener.CLOSE);
    	}
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        e.getCause().printStackTrace();
        PagClientEndpoint ep = PagEndpointManager.getInstance().getEndpointByChannel(e.getChannel());
        if(ep == null) {
        	e.getChannel().close();
        }else {
        	ep.delete();
        }
    }
    
}
