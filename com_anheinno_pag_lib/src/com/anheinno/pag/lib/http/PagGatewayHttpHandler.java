package com.anheinno.pag.lib.http;

import java.net.URI;
import java.util.Collection;
import java.util.Hashtable;
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
		if(path.endsWith("push")) {
			Map<String, List<String>> params = query.getParameters();
			if (!params.isEmpty() && params.containsKey("DEST") && params.containsKey("APP")) {
				Hashtable<String, String> dest_ids = new Hashtable<String, String>();
				String app_id = null;
				String msg_id = null;
				List<String> tmp = params.get("DEST");
				for(int i = 0; i < tmp.size(); i ++) {
					String id = tmp.get(i);
					if(!dest_ids.containsKey(id)) {
						dest_ids.put(id, id);
					}
				}
				tmp = params.get("APP");
				if(tmp != null && tmp.size() > 0) {
					app_id = tmp.get(0);
				}
				if(dest_ids.size() == 1 && params.containsKey("ID")) {
					tmp = params.get("ID");
					if(tmp != null && tmp.size() > 0) {
						msg_id = tmp.get(0);
					}
				}
				Collection<String> enum_ids = dest_ids.values();
				String[] dests = new String[enum_ids.size()];
				enum_ids.toArray(dests);
				if(dests.length > 0 && app_id != null) {
					PagEndpointManager manager = PagEndpointManager.getInstance();					
					if(dests.length == 1) {
						String dest = dests[0];
						PagClientEndpoint ep = manager.getEndpointById(dest);
						if(ep == null) {
							errorResponse(e.getChannel(), HttpResponseStatus.NOT_FOUND, "Destination " + dest + " not found!");
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
							okResponse(e.getChannel());
						}
					}else {
						StringBuffer resp_status = new StringBuffer();
						for(int i = 0; i < dests.length; i ++) {
							PagClientEndpoint ep = manager.getEndpointById(dests[i]);
							if(ep == null) {
								resp_status.append("<status>" + HttpResponseStatus.NOT_FOUND.toString() + "</status>");
							}else {
								PagRequest notify = new PagRequest(ep.getProtocolVersion(), PagMethod.NOTI);
								notify.setDestination(dests[i]);
								notify.setContent(request.getContent());
								if(request.containsHeader(HttpHeaders.Names.CONTENT_TYPE)) {
									notify.addHeader(PagHeaders.Names.CONTENT_TYPE, request.getHeader(HttpHeaders.Names.CONTENT_TYPE));
								}
								ep.sendRequest(notify);
								resp_status.append("<status>" + HttpResponseStatus.OK + "</status>");
							}
						}
						errorResponse(e.getChannel(), HttpResponseStatus.MULTI_STATUS, resp_status.toString());
					}
				}else if(dests.length == 0) {
					errorResponse(e.getChannel(), HttpResponseStatus.BAD_REQUEST, "No DEST specified. Format: push?APP=<app>&DEST=<uri>&ID=<msgid>. DEST can be multiple. ID is optinal and effective only when there is one DEST.");					
				}else if(app_id == null) {
					errorResponse(e.getChannel(), HttpResponseStatus.BAD_REQUEST, "No APP specified. Format: push?APP=<app>&DEST=<uri>&ID=<msgid>. DEST can be multiple. ID is optinal and effective only when there is one DEST.");					
				}
			}else {
				errorResponse(e.getChannel(), HttpResponseStatus.BAD_REQUEST, "Format: push?APP=<app>&DEST=<uri>&ID=<msgid>. DEST can be multiple. ID is optinal and effective only when there is one DEST.");									
			}
		} else if(path.endsWith("register")) {
			Map<String, List<String>> params = query.getParameters();
			if(!params.isEmpty() && params.containsKey("APP") && params.containsKey("URI")) {
				String app_id = null;
				URI uri = null;
				List<String> ids = params.get("APP");
				if(ids != null && ids.size() > 0) {
					app_id = ids.get(0);
				}
				List<String> urls = params.get("URI");
				if(urls != null && urls.size() > 0) {
					try {
						uri = new URI(urls.get(0));
					}catch(final Exception urle) {						
					}
				}
				if(app_id != null && uri != null) {
					PagEndpointManager.getInstance().addHttpEndpoint(app_id, uri);
					okResponse(e.getChannel());
				}else if(app_id == null) {
					errorResponse(e.getChannel(), HttpResponseStatus.BAD_REQUEST, "No APP specified. Format: register?APP=<app>&URI=<uri>");
				}else if(uri == null) {
					errorResponse(e.getChannel(), HttpResponseStatus.BAD_REQUEST, "No URI specified. Format: register?APP=<app>&URI=<uri>");
				}
			}else {
				errorResponse(e.getChannel(), HttpResponseStatus.BAD_REQUEST, "Format: register?APP=<app>&URI=<uri>");				
			}
		} else if(path.endsWith("unregister")) {
			Map<String, List<String>> params = query.getParameters();
			if(!params.isEmpty() && params.containsKey("APP")) {
				String app_id = null;
				List<String> ids = params.get("APP");
				if(ids != null && ids.size() > 0) {
					app_id = ids.get(0);
				}
				if(app_id != null) {
					PagEndpointManager.getInstance().removeHttpEndpoint(app_id);
					okResponse(e.getChannel());
				}else {
					errorResponse(e.getChannel(), HttpResponseStatus.BAD_REQUEST, "No app specified. Format: unregister?APP=<app>");
				}
			}else {
				errorResponse(e.getChannel(), HttpResponseStatus.BAD_REQUEST, "Format: unregister?APP=<app>");
			}
		} else {
			errorResponse(e.getChannel(), HttpResponseStatus.NOT_IMPLEMENTED, "Only push/register/unregister are implemented!");
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
