package com.anheinno.pag.lib.endpoint;

import org.jboss.netty.channel.Channel;

import com.anheinno.pag.lib.codec.PagHeaders;
import com.anheinno.pag.lib.codec.PagRequest;
import com.anheinno.pag.lib.codec.PagVersion;

public class PagMobileEndpoint extends PagClientEndpoint {

	public static final int INIT_INTERVAL = 400; /* in seconds */
	public static final int INC_INTERVAL_STEP = 200; /* in seconds */
	public static final int DEC_INTERVAL_STEP = 100;
	public static final int MIN_INTERVAL = 110; /* in seconds */
	public static final int MAX_INTERVAL = 600; /* in seconds */
	public static final int INTERVAL_KEEP_TIMES = 30;
	
	private int _interval = 0;
	private int _interval_keep_count = 0;
	private MobileEndpointStatus _mobile_status;
	
	enum MobileEndpointStatus {
		NORMAL("I"),
		EXCEPTION("E"),
		CHOKE("C");
		
		private String _status;
		private MobileEndpointStatus(String stat) {
			_status = stat;
		}
		
		public String toString() {
			return _status;
		}
		
		public static MobileEndpointStatus eval(String status) {
			if(status.equals(NORMAL._status)) {
				return NORMAL;
			}else if(status.equals(EXCEPTION._status)) {
				return EXCEPTION;
			}else if(status.equals(CHOKE._status)) {
				return CHOKE;
			}else {
				return null;
			}
		}
	}
	
	public PagMobileEndpoint(String id, Channel channel) {
		super(id, channel);
		_interval = 0;
	}
	
	public PagVersion getProtocolVersion() {
		return PagVersion.PAM_1_0;
	}

	protected boolean expectResponse() {
		return false;
	}

	protected boolean processRegisterRequest(PagRequest request) {
		String ua = request.getHeader(PagHeaders.Names.USER_AGENT);
		if(ua != null) {
			setUA(ua);
		}
		String interval_str = request.getHeader(PagHeaders.Names.INTERVAL);
		int interval = INIT_INTERVAL;
		if(interval_str != null) {
			interval = Integer.parseInt(interval_str);
		}
		System.out.println("init interval: " + interval);
		String status = request.getHeader(PagHeaders.Names.STATUS);
		if(status != null) {
			resetInterval(interval);
			_mobile_status = MobileEndpointStatus.eval(status);
			if(_mobile_status == MobileEndpointStatus.CHOKE) {
				decInterval();
			}
		}else {
			_mobile_status = MobileEndpointStatus.NORMAL;
			resetInterval(interval);
		}
		return true;
	}
	
	private void incInterval() {
		_interval_keep_count++;
		if(_interval_keep_count > INTERVAL_KEEP_TIMES) {
			_interval += INC_INTERVAL_STEP;
			if(_interval > MAX_INTERVAL) {
				_interval = MAX_INTERVAL;
			}
			_interval_keep_count = 0;
		}
		System.out.println("Inc interval to " + _interval);
	}
	
	private void resetInterval(int init_interval) {
		_interval = init_interval;
		_interval_keep_count = INTERVAL_KEEP_TIMES - 1;
		System.out.println("Reset interval to " + _interval);
	}
	
	private void decInterval() {
		_interval -= DEC_INTERVAL_STEP;
		if(_interval < MIN_INTERVAL) {
			_interval = MIN_INTERVAL;
		}
		_interval_keep_count = 0;
		System.out.println("Dec interval to " + _interval);
	}

	protected int getAndUpdateActivateInterval() {
		int invl = _interval;
		incInterval();
		return invl;
	}

}
