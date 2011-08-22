package com.anheinno.pag.lib.endpoint;

import java.net.URI;
import java.util.Hashtable;

import org.jboss.netty.channel.Channel;

public class PagEndpointManager {
	
	private Hashtable<String, PagClientEndpoint> _table_by_id;
	private Hashtable<Integer, PagClientEndpoint> _table_by_channel;
	private Hashtable<String, URI> _http_ep_table;
	
	private PagEndpointManager() {
		_table_by_id = new Hashtable<String, PagClientEndpoint>();
		_table_by_channel = new Hashtable<Integer, PagClientEndpoint>();
		_http_ep_table = new Hashtable<String, URI>();
	}
	
	private static PagEndpointManager _singleton;
	static {
		_singleton = new PagEndpointManager();
	}
	
	public static PagEndpointManager getInstance() {
		return _singleton;
	}
	
	public synchronized PagClientEndpoint getEndpointById(String id) {
		if(_table_by_id.containsKey(id)) {
			return _table_by_id.get(id);
		}else {
			System.out.println("endpoint with ID=" + id + " not exists!");
			return null;
		}
	}
	
	public synchronized PagClientEndpoint getEndpointByChannel(Channel channel) {
		if(_table_by_channel.containsKey(channel.getId())) {
			return _table_by_channel.get(channel.getId());
		}else {
			System.out.println("endpoint with channel=" + channel.getId() + " not exists!");
			return null;
		}
	}
	
	public synchronized void addEndpoint(PagClientEndpoint endpoint) {
		if(_table_by_id.containsKey(endpoint.getID())) {
			System.out.println("addEndpoint: endpoint with ID=" + endpoint.getID() + " exists!!");
		}else if(_table_by_channel.containsKey(endpoint.getChannel())) {
			System.out.println("addEndpoint: endpoint with channel=" + endpoint.getChannel().getId() + " exists!");
		}else {
			System.out.println("addEndpoint: endpoint ID=" + endpoint.getID() + " channel=" + endpoint.getChannel().getId() + " success!");
			_table_by_id.put(endpoint.getID(), endpoint);
			_table_by_channel.put(endpoint.getChannel().getId(), endpoint);	
			endpoint.setManager(this);
		}
	}
	
	public void addHttpEndpoint(String id, URI url) {
		synchronized(_http_ep_table) {
			_http_ep_table.put(id, url);
		}
	}
	
	public synchronized void removeEndpoint(PagClientEndpoint endpoint) {
		if(_table_by_id.containsKey(endpoint.getID()) && _table_by_channel.containsKey(endpoint.getChannel().getId())) {
			_table_by_id.remove(endpoint.getID());
			_table_by_channel.remove(endpoint.getChannel().getId());
			endpoint.setManager(null);
			System.out.println("endpoint " + endpoint.toString() + " cleaned!");
		}else {
			System.out.println("endpoint " + endpoint.toString() + " not exist!");
		}
	}
	
	public void removeHttpEndpoint(String id) {
		synchronized(_http_ep_table) {
			_http_ep_table.remove(id);
		}
	}
	
	public URI getHttpNotifyURI(String id) {
		synchronized(_http_ep_table) {
			if(_http_ep_table.containsKey(id)) {
				return _http_ep_table.get(id);
			}else {
				return null;
			}
		}
	}
	
	public synchronized void replaceEndpointChannel(PagClientEndpoint endpoint, Channel new_channel) {
		if(_table_by_id.containsKey(endpoint.getID()) && _table_by_channel.containsKey(endpoint.getChannel().getId())) {
			//System.out.print("replace channel " + endpoint.getChannel().getId() + " with " + new_channel.getId());
			_table_by_channel.remove(endpoint.getChannel().getId());
			_table_by_channel.put(new_channel.getId(), endpoint);
			endpoint.replaceChannel(new_channel);
		}
	}
	
	public synchronized boolean closeAll() {
		// to do: close all
		return true;
	}
	
}
