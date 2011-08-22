package com.anheinno.pag.lib.endpoint;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.util.CharsetUtil;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;

import com.anheinno.pag.lib.codec.PagMethod;
import com.anheinno.pag.lib.codec.PagRequest;
import com.anheinno.pag.lib.codec.PagResponse;
import com.anheinno.pag.lib.codec.PagResponseStatus;
import com.anheinno.pag.lib.codec.PagVersion;
import com.anheinno.pag.lib.util.TimerUtil;

public abstract class PagEndpoint implements TimerTask {
	private String _id;
	private Channel _channel;
	private PagEndpointStatus _status;
	private Queue<QueuedMessage> _message_queue;
	private long _when_created;
	private Timeout _activate_timer;
	private Timeout _request_timer;
	private String _ua;
	
	public static final int MAX_TRY_TIMES = 3;
	public static final int REQUEST_TIMEOUT_SECONDS = 180; // 3 minuts
	
	enum PagEndpointStatus {
		PAG_ENDPOINT_INIT,
		PAG_ENDPOINT_REG_SENT,
		PAG_ENDPOINT_IDLE,
		PAG_ENDPOINT_REQUEST_SENT,
		PAG_ENDPOINT_BYE_SENT,
		PAG_ENDPOINT_CLOSE
	}
	
	class QueuedMessage {
		PagRequest _message;
		int _try;
		
		QueuedMessage(PagRequest msg) {
			_message = msg;
			_try = 0;
		}
		
		int sendTimes() {
			return _try;
		}
		
		void tried() {
			_try++;
		}
	}
		
	protected PagEndpoint(String id, Channel channel) {
		_id = id;
		_channel = channel;
		_status = PagEndpointStatus.PAG_ENDPOINT_INIT;
		_message_queue = new LinkedList<QueuedMessage>();
		_when_created = System.currentTimeMillis();
		_activate_timer = null;
		_ua = null;
	}
	
	public String getID() {
		return _id;
	}
	
	public void setStatus(PagEndpointStatus status) {
		_status = status;
	}
	
	public PagEndpointStatus getStatus() {
		return _status;
	}
	
	public void setUA(String ua) {
		_ua = ua;
	}
	
	public String getUA(String ua) {
		return _ua;
	}
	
	public Channel getChannel() {
		return _channel;
	}
	
	public void delete() {
		cancelActivateTimer();
		cancelRequestTimer();
		//setStatus(PagEndpointStatus.PAG_ENDPOINT_CLOSE);
		_message_queue.clear();
		_channel.close();
	}
	
	private void cancelActivateTimer() {
		if(_activate_timer != null && (!_activate_timer.isCancelled() || !_activate_timer.isExpired())) {
			_activate_timer.cancel();
		}
		_activate_timer = null;
	}
	private void startActivateTimer(int interval) {
		_activate_timer = TimerUtil.getTimer().newTimeout(this, interval, TimeUnit.SECONDS);
	}
	private void cancelRequestTimer() {
		if(_request_timer != null && (!_request_timer.isCancelled() || !_request_timer.isExpired())) {
			_request_timer.cancel();
		}
		_request_timer = null;
	}
	private void startRequestTimer() {
		_request_timer = TimerUtil.getTimer().newTimeout(this, REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
	}
	
	public abstract PagVersion getProtocolVersion();
	
	public void handleRequestMessage(PagRequest request) {
		if(getStatus().equals(PagEndpointStatus.PAG_ENDPOINT_INIT)) {
			if(request.getMethod().equals(PagMethod.REG)) {
				handleRegisterRequest(request);
			} else {
				sendResponse(PagResponseStatus.METHOD_NOT_ALLOWED);
			}
		}else if(getStatus().equals(PagEndpointStatus.PAG_ENDPOINT_IDLE) || getStatus().equals(PagEndpointStatus.PAG_ENDPOINT_REQUEST_SENT) || getStatus().equals(PagEndpointStatus.PAG_ENDPOINT_BYE_SENT)) {
			if(request.getMethod().equals(PagMethod.ACT)) {
				handleActivateRequest(request);
			} else if(request.getMethod().equals(PagMethod.NOTI)) {
				handleNotifyRequest(request);
			} else if(request.getMethod().equals(PagMethod.STAT)) {
				handleStatusRequest(request);
			} else if(request.getMethod().equals(PagMethod.ACK)) {
				handleAcknowledgeRequest(request);
			} else if(request.getMethod().equals(PagMethod.BYE)) {
				handleByeRequest(request);
			} else {
				sendResponse(PagResponseStatus.METHOD_NOT_ALLOWED);
			}
		}else {
			sendResponse(PagResponseStatus.METHOD_NOT_ALLOWED);
		}
	}
	
	public ChannelFuture sendResponse(PagResponseStatus status) {
		return sendResponse(status, null);
	}
	
	public ChannelFuture sendResponse(PagResponseStatus status, String msg) {
		PagResponse response = new PagResponse(getProtocolVersion(), status);
		if(msg != null && msg.length() > 0) {
			response.setContent(ChannelBuffers.copiedBuffer(msg, CharsetUtil.UTF_8));
		}
		return _channel.write(response);
	}
	
	protected void handleRegisterRequest(PagRequest request) {
	}
	
	protected void handleNotifyRequest(PagRequest request) {
	}
	
	protected void handleStatusRequest(PagRequest request) {
	}
	
	protected void handleAcknowledgeRequest(PagRequest request) {
	}
	
	protected void handleActivateRequest(PagRequest request) {
		sendResponse(PagResponseStatus.OK);
	}

	protected void handleByeRequest(PagRequest request) {
		setStatus(PagEndpointStatus.PAG_ENDPOINT_CLOSE);
		ChannelFuture future = sendResponse(PagResponseStatus.OK);
		future.addListener(new CloseEndpointChannelFutureListener(this));
	}
	
	protected abstract boolean expectResponse();
	
	public void handleResponseMessage(PagResponse response) {
		cancelRequestTimer();
		if(getStatus().equals(PagEndpointStatus.PAG_ENDPOINT_REG_SENT)
				|| getStatus().equals(PagEndpointStatus.PAG_ENDPOINT_REQUEST_SENT)
				|| getStatus().equals(PagEndpointStatus.PAG_ENDPOINT_BYE_SENT)) {
			synchronized(_message_queue) {
				if(_message_queue.size() > 0) {
					QueuedMessage msgq = _message_queue.peek();
					if(msgq.sendTimes() > 0) {
						if(response != null && response.getStatus().getCode() < 400) {
							if(getStatus().equals(PagEndpointStatus.PAG_ENDPOINT_REG_SENT)
									&& msgq._message.getMethod().equals(PagMethod.REG)) {
								_message_queue.poll();
								setStatus(PagEndpointStatus.PAG_ENDPOINT_IDLE);
								doSendRequest();
							}else if(getStatus().equals(PagEndpointStatus.PAG_ENDPOINT_BYE_SENT)
									&& msgq._message.getMethod().equals(PagMethod.BYE)) {
								_message_queue.poll();
								setStatus(PagEndpointStatus.PAG_ENDPOINT_CLOSE);
								delete();
							}else {
								_message_queue.poll();
								setStatus(PagEndpointStatus.PAG_ENDPOINT_IDLE);
								doSendRequest();
							}
						} else {
							if(getStatus().equals(PagEndpointStatus.PAG_ENDPOINT_REG_SENT)
									&& msgq._message.getMethod().equals(PagMethod.REG)) {
								setStatus(PagEndpointStatus.PAG_ENDPOINT_CLOSE);
								delete();
							}else if(msgq.sendTimes() < MAX_TRY_TIMES) {
								doSendRequest();
							}else {
								setStatus(PagEndpointStatus.PAG_ENDPOINT_CLOSE);
								delete();
							}
						}
					}
				}
			}
		}
	}
	
	protected void sendActivateRequest() {
		PagRequest act = new PagRequest(getProtocolVersion(), PagMethod.ACT);
		int interval = getAndUpdateActivateInterval();
		act.setDestination("" + interval);
		sendRequest(act);
		
		startActivateTimer(interval);
	}
	
	public void run(Timeout timer) throws Exception {
		if(_channel.isConnected()) {
			if(_activate_timer != null && timer == _activate_timer) {
				sendActivateRequest();
			}else if(_request_timer != null && timer == _request_timer) {
				handleResponseMessage(null);
			}
		}else {
			System.out.println("Connection closed!!");
			setStatus(PagEndpointStatus.PAG_ENDPOINT_CLOSE);
			delete();
		}
		
	}
	
	protected abstract int getAndUpdateActivateInterval();
	
	public void sendRequest(PagRequest request) {
		synchronized(_message_queue) {
			QueuedMessage msgq = new QueuedMessage(request);
			_message_queue.offer(msgq);
			doSendRequest();
		}
	}
	
	public long getLiveTime() {
		return System.currentTimeMillis() - _when_created;
	}
	
	private void doSendRequest() {
		if(_message_queue.size() > 0) {
			QueuedMessage msgq = _message_queue.peek();
			if(msgq != null) {
				System.out.println("Send out: " + msgq._message);
				_channel.write(msgq._message);
				msgq.tried();
				if(!expectResponse()) {
					setStatus(PagEndpointStatus.PAG_ENDPOINT_IDLE);
					_message_queue.poll();
				}else {
					if(msgq._message.getMethod().equals(PagMethod.REG)) {
						setStatus(PagEndpointStatus.PAG_ENDPOINT_REG_SENT);
					}else if(msgq._message.getMethod().equals(PagMethod.BYE)) {
						setStatus(PagEndpointStatus.PAG_ENDPOINT_BYE_SENT);
					}else {
						setStatus(PagEndpointStatus.PAG_ENDPOINT_REQUEST_SENT);
					}
					startRequestTimer();
				}
			}
		}else {
			setStatus(PagEndpointStatus.PAG_ENDPOINT_IDLE);
		}
	}
	
	public synchronized void replaceChannel(Channel ch) {
		synchronized(_message_queue) {
			//System.out.println("replaceChannel " + _channel.getId() + " with " + ch.getId());
			this._channel.close();
			this._channel = ch;
			setStatus(PagEndpointStatus.PAG_ENDPOINT_INIT);
		}
	}
	
	public String toString() {
		return getID() + "#" + getChannel().getId();
	}
	
	class CloseEndpointChannelFutureListener implements ChannelFutureListener {
		private PagEndpoint _endpoint;
		
		CloseEndpointChannelFutureListener(PagEndpoint ep) {
			_endpoint = ep;
		}
		
		public void operationComplete(ChannelFuture arg0) throws Exception {
			_endpoint.delete();
		}
		
	}
	
}
