package com.anheinno.pag.server.handler;

import java.net.InetSocketAddress;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFactory;

public class PagServer {
	private int _pag_port;
	private ServerBootstrap _pag_server;
	
	public PagServer(int port) {
		_pag_port = port;
	}
	
	public void start(ChannelFactory factory) {
		_pag_server = new ServerBootstrap (factory);
		
        _pag_server.setPipelineFactory(new PagServerPipelineFactory());

        _pag_server.setOption("child.tcpNoDelay", true);
        _pag_server.setOption("child.keepAlive", true);

        _pag_server.bind(new InetSocketAddress(_pag_port));
        
        System.out.println("Server Started at " + _pag_port);
	}
	
	public void stop() {
		_pag_server.releaseExternalResources();
	}
}