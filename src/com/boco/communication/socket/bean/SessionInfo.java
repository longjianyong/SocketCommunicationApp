package com.boco.communication.socket.bean;

import java.util.HashMap;
import java.util.Map;

import com.boco.communication.socket.util.RegisteEvent;

/**
 * @author LongJianYong
 * @date 2014-3-21 ����04:00:55
 * @version V1.0
 * @description 
 */
public class SessionInfo {

	// ���������������ٸ��ͻ����߳��ڷ��ʸ�����
	private int sessionCount = 0;
	
//	����Ự���ͣ���������1�������ӡ�2�������ӣ���Ĭ�ϳ�����
	private int sessionType = 1;
	
	// ���������������ӶϿ���ʱ����Զ���������������������Ϊ0��ʹ�õ��Ƕ���������Ҫ����
	private int reconnectCount = 0;
	
	// Զ�����ӵ�ַ
	private String hostName;

	// Զ�����Ӷ˿�
	private int port;
	
//	�Ự�������ʵ������
	private String handlerClass;
	
	//	�������Ӻ���Ҫע����¼�����
	public Map<Integer, RegisteEvent> connectedEventMap = new HashMap<Integer,RegisteEvent>();
	
	public SessionInfo () {
	}

	public SessionInfo (String hostName, int port) {
		this.hostName = hostName;
		this.port = port;
	}
	
	public int getSessionType() {
		return sessionType;
	}

	public void setSessionType(int sessionType) {
		this.sessionType = sessionType;
	}

	public String getHandlerClass() {
		return handlerClass;
	}

	public void setHandlerClass(String handlerClass) {
		this.handlerClass = handlerClass;
	}

	public int getSessionCount() {
		return sessionCount;
	}

	public void setSessionCount(int sessionCount) {
		this.sessionCount = sessionCount;
	}

	public synchronized void addSessionCount() {
		++this.sessionCount;
	}
	
	public synchronized void delSessionCount() {
		--this.sessionCount;
	}

	public int getReconnectCount() {
		return reconnectCount;
	}

	public void setReconnectCount(int reconnectCount) {
		this.reconnectCount = reconnectCount;
	}

	public synchronized void addReconnectCount() {
		++this.reconnectCount;
	}
	
	public synchronized void delReconnectCount() {
		--this.reconnectCount;
	}
	
	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public boolean compareTo(SessionInfo o) {
		if (o.hostName == this.hostName && o.port == this.port){
			return true;
		}
		else
			return false;
	}

}
