package com.anheinno.pag.lib.endpoint;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.handler.codec.http.HttpHeaders;

import com.anheinno.pag.lib.codec.PagHeaders;
import com.anheinno.pag.lib.codec.PagRequest;
import com.anheinno.pag.lib.codec.PagResponseStatus;
import com.anheinno.pag.lib.http.PagGatewayHttpUtility;

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
			if(relayHttpRequest(uri, request)) {
				sendResponse(PagResponseStatus.OK);
			}else {
				sendResponse(PagResponseStatus.INTERNAL_SERVER_ERROR);
			}
		}else {
			sendResponse(PagResponseStatus.NOT_FOUND);
		}
	}
	
	private boolean relayHttpRequest(URI uri, PagRequest request) {
		try {
			if(uri.getScheme().equalsIgnoreCase("https")) {
				PagGatewayHttpUtility.installTrustAllSSLFactory();
			}
			
			HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
			
			conn.setUseCaches(false);
			conn.setDoInput(false);
			
			ChannelBuffer content = request.getContent();

			if (content.readable()) {
				conn.setRequestMethod("POST");
				
				if(request.containsHeader(PagHeaders.Names.CONTENT_TYPE)) {
					conn.setRequestProperty(HttpHeaders.Names.CONTENT_TYPE,
						request.getHeader(PagHeaders.Names.CONTENT_TYPE));
				}
				int len = content.readableBytes();
				byte[] content_bytes = new byte[len];
				conn.setRequestProperty(HttpHeaders.Names.CONTENT_LENGTH, "" + len);

				conn.setDoOutput(true);

				OutputStream output = null;
				try {
					output = conn.getOutputStream();
					output.write(content_bytes);
				} finally {
					if (output != null) {
						try {
							output.close();
						} catch (final IOException logOrIgnore) {
						}
					}
				}
			} else {
				conn.setRequestMethod("GET");
			}

			conn.setRequestProperty(PagGatewayHttpUtility.getHeaderPag2Http("METHOD"), request.getMethod().getName());
			if(request.containsHeader(PagHeaders.Names.MSGID)) {
				conn.setRequestProperty(PagGatewayHttpUtility.getHeaderPag2Http(PagHeaders.Names.MSGID), request.getHeader(PagHeaders.Names.MSGID));
			}
			
			if(conn.getResponseCode() >= 200 && conn.getResponseCode() < 300) {
				return true;
			}
		}catch(final Exception e) {
			
		}
		return false;
	}
	
}
