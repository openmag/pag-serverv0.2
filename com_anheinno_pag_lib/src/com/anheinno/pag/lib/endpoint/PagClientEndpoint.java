package com.anheinno.pag.lib.endpoint;

import java.net.URI;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;

import com.anheinno.pag.lib.codec.PagHeaders;
import com.anheinno.pag.lib.codec.PagRequest;
import com.anheinno.pag.lib.codec.PagResponseStatus;

public abstract class PagClientEndpoint extends PagEndpoint {
	private PagEndpointManager _manager;

	protected PagClientEndpoint(String id, Channel channel) {
		super(id, channel);
		_manager = null;
	}
	
	public void setManager(PagEndpointManager manager) {
		_manager = manager;
	}
	
	public void delete() {
		_manager.removeEndpoint(this);
		super.delete();
	}
	
	protected void handleRegisterRequest(PagRequest request) {
		if(processRegisterRequest(request)) {
			if(getStatus() == PagEndpointStatus.PAG_ENDPOINT_INIT) {
				setStatus(PagEndpointStatus.PAG_ENDPOINT_IDLE);
			}
			ChannelFuture future = sendResponse(PagResponseStatus.OK);
			future.addListener(new ChannelFutureListener() {
				public void operationComplete(ChannelFuture future)
						throws Exception {
					sendActivateRequest();
				}
			});			
		}else {
			ChannelFuture future = sendResponse(PagResponseStatus.BAD_REQUEST, "Register failed!");
			future.addListener(new CloseEndpointChannelFutureListener(this));
		}
	}
	
	protected abstract boolean processRegisterRequest(PagRequest request);
	
	protected void handleStatusRequest(PagRequest request) {		
		PagEndpoint ep = _manager.getEndpointById(request.getDestination());
		if(ep != null) {
			long live_time = getLiveTime();
			sendResponse(PagResponseStatus.OK, "" + live_time);
		}else {
			sendResponse(PagResponseStatus.NOT_FOUND);
		}
	}
	
	protected void handleNotifyRequest(PagRequest request) {
		if(request.getContent().equals(ChannelBuffers.EMPTY_BUFFER)) {
			sendResponse(PagResponseStatus.BAD_REQUEST, "NOTI must carry content!");
		}
		/*else if(!request.containsHeader(PagHeaders.Names.MSGID)) {
			sendResponse(PagResponseStatus.BAD_REQUEST, "NOTI must carry MSGID!");
		}*/
		else {
			relayRequest(request);
		}
	}
	
	protected void handleAcknowledgeRequest(PagRequest request) {
		if(!request.containsHeader(PagHeaders.Names.MSGID)) {
			sendResponse(PagResponseStatus.BAD_REQUEST, "ACK must carry MSGID!");
		}else {
			relayRequest(request);
		}
	}
	
	private void relayRequest(PagRequest request) {
		String dst_id = request.getDestination();
		request.setDestination(getID());
		PagEndpoint ep = _manager.getEndpointById(dst_id);
		if(ep != null) {
			sendResponse(PagResponseStatus.OK);
			request.setProtocolVersion(ep.getProtocolVersion());
			ep.sendRequest(request);
		}else if(_manager.getHttpNotifyURI(dst_id) != null) {
			URI uri = _manager.getHttpNotifyURI(dst_id);

		}else {
			sendResponse(PagResponseStatus.NOT_FOUND);
		}
	}
	
}
