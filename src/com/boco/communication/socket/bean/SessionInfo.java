package com.boco.communication.socket.bean;

import java.util.HashMap;
import java.util.Map;

import com.boco.communication.socket.util.RegisteEvent;

/**
 * @author LongJianYong
 * @date 2014-3-21 下午04:00:55
 * @version V1.0
 * @description 
 */
public class SessionInfo {

	// 请求连接数，多少个客户端线程在访问该链接
	private int sessionCount = 0;
	
//	请求会话类型（用于区分1、长链接、2、短链接），默认长链接
	private int sessionType = 1;
	
	// 重连次数，当连接断开的时候会自动重连，如果上面的连接数为0且使用的是短连接则不需要重连
	private int reconnectCount = 0;
	
	// 远程连接地址
	private String hostName;

	// 远程连接端口
	private int port;
	
//	会话结果处理实现类名
	private String handlerClass;
	
	//	建立链接后需要注册的事件集合
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
