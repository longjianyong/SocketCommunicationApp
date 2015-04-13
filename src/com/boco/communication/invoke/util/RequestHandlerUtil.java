package com.boco.communication.invoke.util;

import com.boco.communication.socket.bean.SessionInfo;
import com.boco.communication.socket.business.RequestSessionIF;
import com.boco.communication.socket.business.impl.RequestSessionImpl;
import com.boco.communication.socket.intf.CommunicationClientIF;

public class RequestHandlerUtil {
	
	private static RequestSessionIF handler = new RequestSessionImpl();

	public static void send(SessionInfo sInfo, int type, byte[] data) {
		
		CommunicationClientIF client = null;
		if(sInfo.getSessionType() == 1){
			client = LoadBalanceUtil.loadBalanceChannel(sInfo);
			if(client == null)
				return;
		}else{
			client = LoadBalanceUtil.initChannelThread(sInfo);
		}
		handler.send(client, type, data);
	}
	
	public static boolean closeSession(SessionInfo sessionInfo){
		return handler.closeSession(sessionInfo);
	}
	
}
