package com.anheinno.pag.server;

import java.util.concurrent.Executors;

import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import com.anheinno.pag.lib.console.ConsoleService;
import com.anheinno.pag.lib.http.PagGatewayHttpServer;
import com.anheinno.pag.server.handler.PagServer;

public class PagServerApp {
	private PagServer _pag_server;
	private PagGatewayHttpServer _http_server;
	private ConsoleService _console_server;
	
	PagServerApp(int pag_port, int http_port, int admin_port) {
		_pag_server = new PagServer(pag_port);
		_http_server = new PagGatewayHttpServer(http_port);
		_console_server = new ConsoleService(admin_port);
	}
	
	void start() {
		ChannelFactory factory = new NioServerSocketChannelFactory (
				Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
		
		_pag_server.start(factory);
		_http_server.start(factory);
        _console_server.start(factory);
	}
	
	void stop() {
		_pag_server.stop();
		_http_server.stop();
		_console_server.stop();
	}
	
	public static void main(String[] args) throws Exception {
		int pag_port = 12333;
		int http_port = 28080;
		int admin_port = 12334;
		
		PagServerApp app = new PagServerApp(pag_port, http_port, admin_port);
		app.start();
	}
	
}
