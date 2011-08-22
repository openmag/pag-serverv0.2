package com.anheinno.pag.lib.endpoint;

import org.jboss.netty.channel.Channel;

import com.anheinno.pag.lib.codec.PagRequest;
import com.anheinno.pag.lib.codec.PagVersion;

public class PagApplicationEndpoint extends PagClientEndpoint {

	private int _interval = 5*60;
	
	public PagApplicationEndpoint(String id, Channel channel) {
		super(id, channel);
		
	}

	public PagVersion getProtocolVersion() {
		return PagVersion.PAA_1_0;
	}

	protected boolean expectResponse() {
		return true;
	}

	protected boolean processRegisterRequest(PagRequest request) {
		return true;
	}

	/**
	 * In seconds
	 */
	protected int getAndUpdateActivateInterval() {
		return _interval;
	}

}
