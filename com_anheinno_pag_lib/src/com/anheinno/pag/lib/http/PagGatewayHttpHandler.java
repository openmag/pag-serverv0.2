package com.anheinno.pag.lib.http;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
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
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.jboss.netty.util.CharsetUtil;

import com.anheinno.pag.lib.codec.PagHeaders;
import com.anheinno.pag.lib.codec.PagMethod;
import com.anheinno.pag.lib.codec.PagRequest;
import com.anheinno.pag.lib.endpoint.PagClientEndpoint;
import com.anheinno.pag.lib.endpoint.PagEndpointManager;

public class PagGatewayHttpHandler extends SimpleChannelUpstreamHandler {

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		HttpRequest request = (HttpRequest) e.getMessage();

		QueryStringDecoder query = new QueryStringDecoder(request.getUri());
		String path = query.getPath();
		Map<String, List<String>> params = query.getParameters();
		if(path.endsWith("noti") || path.endsWith("push")) {
			push(request, params, e.getChannel());			
		} else if(path.endsWith("ack")) {
			ack(request, params, e.getChannel());
		} else if(path.endsWith("register")) {
			register(request, params, e.getChannel());
		} else if(path.endsWith("unregister")) {
			unregister(request, params, e.getChannel());
		} else {
			errorResponse(e.getChannel(), HttpResponseStatus.NOT_IMPLEMENTED, "Only push/register/unregister are implemented!");
		}
	}
	
	private void push(HttpRequest request, Map<String, List<String>> params, Channel ch) {
		String[] dest_ids = PagGatewayHttpUtility.getParams(params, "DEST");
		String app_id =  PagGatewayHttpUtility.getParam(params, "APP");
		String msg_id = null;
		if(dest_ids.length == 1 && request.containsHeader(PagGatewayHttpUtility.getHeaderPag2Http(PagHeaders.Names.MSGID))) {
			msg_id = request.getHeader(PagGatewayHttpUtility.getHeaderPag2Http(PagHeaders.Names.MSGID));
		}
		if(app_id != null && request.containsHeader(PagGatewayHttpUtility.getHeaderPag2Http("URI"))) {
			String noti_uri = request.getHeader(PagGatewayHttpUtility.getHeaderPag2Http("URI"));
			try {
				PagEndpointManager.getInstance().addHttpEndpoint(app_id, new URI(noti_uri));
			}catch(final Exception uri_e) {
			}
		}
		if(dest_ids != null && dest_ids.length > 0 && app_id != null) {
			PagEndpointManager manager = PagEndpointManager.getInstance();					
			if(dest_ids.length == 1) {
				String dest = dest_ids[0];
				PagClientEndpoint ep = manager.getEndpointById(dest);
				if(ep == null) {
					errorResponse(ch, HttpResponseStatus.NOT_FOUND, "Destination " + dest + " not found!");
				}else {
					PagRequest notify = new PagRequest(ep.getProtocolVersion(), PagMethod.NOTI);
					notify.setDestination(dest);
					if(msg_id != null) {
						notify.addHeader(PagHeaders.Names.MSGID, msg_id);
					}
					if(request.containsHeader(HttpHeaders.Names.CONTENT_TYPE)) {
						notify.addHeader(PagHeaders.Names.CONTENT_TYPE, request.getHeader(HttpHeaders.Names.CONTENT_TYPE));
					}
					notify.setContent(request.getContent());
					ep.sendRequest(notify);
					okResponse(ch);
				}
			}else {
				StringBuffer resp_status = new StringBuffer();
				for(int i = 0; i < dest_ids.length; i ++) {
					PagClientEndpoint ep = manager.getEndpointById(dest_ids[i]);
					if(ep == null) {
						resp_status.append("<status>" + HttpResponseStatus.NOT_FOUND.toString() + "</status>");
					}else {
						PagRequest notify = new PagRequest(ep.getProtocolVersion(), PagMethod.NOTI);
						notify.setDestination(dest_ids[i]);
						notify.setContent(request.getContent());
						if(request.containsHeader(HttpHeaders.Names.CONTENT_TYPE)) {
							notify.addHeader(PagHeaders.Names.CONTENT_TYPE, request.getHeader(HttpHeaders.Names.CONTENT_TYPE));
						}
						ep.sendRequest(notify);
						resp_status.append("<status>" + HttpResponseStatus.OK + "</status>");
					}
				}
				errorResponse(ch, HttpResponseStatus.MULTI_STATUS, resp_status.toString());
			}
		}else {
			errorResponse(ch, HttpResponseStatus.BAD_REQUEST, "Format: noti?APP=<app>&DEST=<uri>. DEST can be multiple. Header X-Anhe-PAG-MSGID is optinal and effective only when there is one DEST.");									
		}
	}
	
	private void ack(HttpRequest request, Map<String, List<String>> params, Channel ch) {
		String app_id = PagGatewayHttpUtility.getParam(params, "APP");
		String dest_id = PagGatewayHttpUtility.getParam(params, "DEST");
		String msg_id = null;
		if(request.containsHeader(PagGatewayHttpUtility.getHeaderPag2Http(PagHeaders.Names.MSGID))) {
			msg_id = request.getHeader(PagGatewayHttpUtility.getHeaderPag2Http(PagHeaders.Names.MSGID));
		}
		if(app_id != null && request.containsHeader(PagGatewayHttpUtility.getHeaderPag2Http("URI"))) {
			String noti_uri = request.getHeader(PagGatewayHttpUtility.getHeaderPag2Http("URI"));
			try {
				PagEndpointManager.getInstance().addHttpEndpoint(app_id, new URI(noti_uri));
			}catch(final Exception uri_e) {
			}
		}
		if(app_id != null && dest_id != null && msg_id != null) {
			PagEndpointManager manager = PagEndpointManager.getInstance();
			PagClientEndpoint ep = manager.getEndpointById(dest_id);
			if(ep == null) {
				errorResponse(ch, HttpResponseStatus.NOT_FOUND, "Destination " + dest_id + " not found!");
			}else {
				PagRequest ack = new PagRequest(ep.getProtocolVersion(), PagMethod.ACK);
				ack.setDestination(dest_id);
				ack.addHeader(PagHeaders.Names.MSGID, msg_id);
				ep.sendRequest(ack);
				okResponse(ch);
			}
		}else {
			errorResponse(ch, HttpResponseStatus.BAD_REQUEST, "Format: ack?APP=<app>&DEST=<uri>. Header X-Anhe-PAG-MSGID must present.");			
		}
	}
	
	private void register(HttpRequest request, Map<String, List<String>> params, Channel ch) {
		String app_id = PagGatewayHttpUtility.getParam(params, "APP");
		URI uri = null;
		if(app_id != null && request.containsHeader(PagGatewayHttpUtility.getHeaderPag2Http("URI"))) {
			String noti_uri = request.getHeader(PagGatewayHttpUtility.getHeaderPag2Http("URI"));
			try {
				uri = new URI(noti_uri);
			}catch(final Exception uri_e) {
			}
		}
		if(app_id != null && uri != null) {
			PagEndpointManager.getInstance().addHttpEndpoint(app_id, uri);
			okResponse(ch);
		} else {
			errorResponse(ch, HttpResponseStatus.BAD_REQUEST, "Format: register?APP=<app>. Header X-Anhe-PAG-URI must present.");
		}
	}
	
	private void unregister(HttpRequest request, Map<String, List<String>> params, Channel ch) {
		String app_id = PagGatewayHttpUtility.getParam(params, "APP");
		if(app_id != null) {
			PagEndpointManager.getInstance().removeHttpEndpoint(app_id);
			okResponse(ch);
		}else {
			errorResponse(ch, HttpResponseStatus.BAD_REQUEST, "No app specified. Format: unregister?APP=<app>");
		}
	}

	private void errorResponse(Channel ch, HttpResponseStatus status, String msg) {
		HttpResponse resp = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);
		resp.addHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
		resp.addHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain;charset=utf-8");
		resp.setContent(ChannelBuffers.copiedBuffer(msg, CharsetUtil.UTF_8));
		ChannelFuture future = ch.write(resp);
		future.addListener(ChannelFutureListener.CLOSE);
	}
	
	private void okResponse(Channel ch) {
		HttpResponse resp = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
		resp.addHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain;charset=utf-8");
		resp.setContent(ChannelBuffers.EMPTY_BUFFER);
		String msg = "OK";
		resp.addHeader(HttpHeaders.Names.CONTENT_LENGTH, msg.length());
		resp.setContent(ChannelBuffers.copiedBuffer(msg, CharsetUtil.UTF_8));
		ch.write(resp);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		e.getCause().printStackTrace();
		e.getChannel().close();
	}
	
}
