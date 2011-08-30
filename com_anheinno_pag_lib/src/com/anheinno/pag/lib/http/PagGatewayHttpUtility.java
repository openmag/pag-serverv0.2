package com.anheinno.pag.lib.http;

import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class PagGatewayHttpUtility {
	public static String getHeaderPag2Http(String pag_header) {
		return "X-Anhe-PAG-" + pag_header;
	}
	
	private static boolean _trust_all_init = false;
	public static void installTrustAllSSLFactory() {
		if(!_trust_all_init) {
			// Create a trust manager that does not validate certificate chains
			TrustManager[] trustAllCerts = new TrustManager[] { 
					new X509TrustManager() {
						public java.security.cert.X509Certificate[] getAcceptedIssuers() {
							return null;
						}
	
						public void checkClientTrusted(
								java.security.cert.X509Certificate[] certs, String authType) {
						}
	
						public void checkServerTrusted(
								java.security.cert.X509Certificate[] certs, String authType) {
						}
					}
			};
	
			// Install the all-trusting trust manager
			try {
				SSLContext sc = SSLContext.getInstance("SSL");
				sc.init(null, trustAllCerts, new java.security.SecureRandom());
				HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
				HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
					public boolean verify(String arg0, SSLSession arg1) {
						return true;
					}
				});
			} catch (Exception e) {
			}
			_trust_all_init = true;
		}
	}
	
	public static String getParam(Map<String, List<String>> params, String name) {
		if(!params.containsKey(name)) {
			return null;
		}
		List<String> tmp = params.get(name);
		if(tmp != null && tmp.size() > 0) {
			return tmp.get(0);
		}else {
			return null;
		}
	}
	
	public static String[] getParams(Map<String, List<String>> params, String name) {
		if(!params.containsKey(name)) {
			return null;
		}
		Hashtable<String, String> dest_ids = new Hashtable<String, String>();
		List<String> tmp = params.get(name);
		for(int i = 0; tmp != null && i < tmp.size(); i ++) {
			String id = tmp.get(i);
			if(!dest_ids.containsKey(id)) {
				dest_ids.put(id, id);
			}
		}
		if(dest_ids.size() > 0) {
			Collection<String> enum_ids = dest_ids.values();
			String[] dests = new String[enum_ids.size()];
			enum_ids.toArray(dests);
			return dests;
		}else {
			return null;
		}
	}
}
