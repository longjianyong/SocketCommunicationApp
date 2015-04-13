package com.boco.communication.socket.business;

import com.boco.communication.socket.bean.SessionInfo;
import com.boco.communication.socket.intf.CommunicationClientIF;

/**
 * @author Longjianyong
 *	业务处理接口
 */
public interface RequestSessionIF {

	public boolean send(CommunicationClientIF client, int type, byte[] data);
	
	public boolean closeSession(SessionInfo siBean);
	
}
