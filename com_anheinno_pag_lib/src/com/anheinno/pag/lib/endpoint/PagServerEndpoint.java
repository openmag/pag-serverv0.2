package com.anheinno.pag.lib.endpoint;

import org.jboss.netty.channel.Channel;

import com.anheinno.pag.lib.codec.PagMethod;
import com.anheinno.pag.lib.codec.PagRequest;
import com.anheinno.pag.lib.codec.PagVersion;

public class PagServerEndpoint extends PagEndpoint {
	private int _interval;
	
	public PagServerEndpoint(String id, Channel channel) {
		super(id, channel);
		_interval = 5;
	}

	public PagVersion getProtocolVersion() {
		return PagVersion.PAA_1_0;
	}

	protected boolean expectResponse() {
		return true;
	}

	@Override
	protected int getAndUpdateActivateInterval() {
		return _interval;
	}
	
	public void reigster() {
		PagRequest reg = new PagRequest(getProtocolVersion(), PagMethod.REG);
		reg.setDestination(getID());
		super.sendRequest(reg);
	}
	
	protected void handleNotifyRequest(PagRequest request) {
	}

	protected void handleAcknowledgeRequest(PagRequest request) {
	}

}
