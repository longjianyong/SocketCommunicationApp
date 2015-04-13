package com.boco.communication.socket.business;

import com.boco.communication.socket.bean.SessionInfo;
import com.boco.communication.socket.intf.CommunicationClientIF;

/**
 * @author Longjianyong
 *	ҵ����ӿ�
 */
public interface RequestSessionIF {

	public boolean send(CommunicationClientIF client, int type, byte[] data);
	
	public boolean closeSession(SessionInfo siBean);
	
}
