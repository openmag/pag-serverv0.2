package com.anheinno.pag.lib.http;

import java.net.InetSocketAddress;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFactory;


public class PagGatewayHttpServer {
	private int _http_port;
	private ServerBootstrap _pag_server;
	
	public PagGatewayHttpServer(int port) {
		_http_port = port;
	}
	
	public void start(ChannelFactory factory) {
		_pag_server = new ServerBootstrap (factory);
		
        _pag_server.setPipelineFactory(new PagGatewayHttpPipelineFactory());

        _pag_server.setOption("child.tcpNoDelay", true);
        _pag_server.setOption("child.keepAlive", true);

        _pag_server.bind(new InetSocketAddress(_http_port));
        
        System.out.println("HTTP Server Started at " + _http_port);
	}
	
	public void stop() {
		_pag_server.releaseExternalResources();
	}
}
