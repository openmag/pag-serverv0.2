package com.anheinno.pag.lib.util;

import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;

public class TimerUtil {
	private static HashedWheelTimer _timer = null;
	
	public static Timer getTimer() {
		if(_timer == null) {
			_timer = new HashedWheelTimer();
		}
		return _timer;
	}
}
