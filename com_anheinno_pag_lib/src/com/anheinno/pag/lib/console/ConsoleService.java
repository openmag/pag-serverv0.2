package com.anheinno.pag.lib.console;

import java.net.InetSocketAddress;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;

public class ConsoleService {
	private int _port;
	private ServerBootstrap _console_server;
	
	public ConsoleService(int port) {
		_port = port;
	}
	
	public boolean start(ChannelFactory factory) {
		_console_server = new ServerBootstrap (factory);
		
        _console_server.setPipelineFactory(new ConsoleServerPipelineFactory());

        _console_server.setOption("child.tcpNoDelay", true);
        _console_server.setOption("child.keepAlive", true);

        Channel ch = _console_server.bind(new InetSocketAddress(_port));
        
        System.out.println("Console Started at " + _port);
        
        return ch.isBound();
	}
	
	public void stop() {
		_console_server.releaseExternalResources();
	}
}
