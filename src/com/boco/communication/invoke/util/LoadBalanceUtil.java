package com.boco.communication.invoke.util;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.boco.communication.socket.bean.SessionInfo;
import com.boco.communication.socket.intf.CommunicationClientIF;
import com.boco.communication.socket.intf.impl.CommunicationClientImpl;

/**
 * @author LongJianYong 1、初始化加载通道线程 2、负载均衡简单实现
 */
public class LoadBalanceUtil {

	/**
	 * @param sInfo
	 * @return 负载均衡客户端通道
	 */
	public static CommunicationClientIF loadBalanceChannel(SessionInfo sInfo) {
		Map<String, CommunicationClientImpl> serverBeanMap = CommunicationClientImpl
				.getServerBeanMap();
		CommunicationClientIF communicationClient = null;
		if (sInfo != null
				&& serverBeanMap.containsKey(sInfo.getHostName()
						+ sInfo.getPort())) {
			communicationClient = serverBeanMap.get(sInfo.getHostName()
					+ sInfo.getPort());
		} else {
			communicationClient = getLoadBalanceClient(serverBeanMap.values());
		}
		return communicationClient;
	}

	/**
	 * @param socketList
	 * @return 简单的负载均衡，返回连接数最少的客户端对象
	 */
	private static CommunicationClientIF getLoadBalanceClient(
			Collection<CommunicationClientImpl> socketList) {
		CommunicationClientIF info = null;
		for (CommunicationClientIF client : socketList) {
			if (!client.isConnected()) // 如果当前链接无效，跳过
				continue;
			if (client.getSiBean().getSessionCount() == 0)
				return client;
			if (info == null)
				info = client;
			else if (client != null) {
				info = info.getSiBean().getSessionCount() <= client.getSiBean()
						.getSessionCount() ? info : client;
			}
		}
		return info;
	}

	/**
	 * @param sessionInfoList
	 *            初始化加载需要启动的通道线程
	 */
	public static void initChannelThread(List<SessionInfo> sessionInfoList) {
		for (SessionInfo sessionInfo : sessionInfoList) {
			initChannelThread(sessionInfo);
		}
	}

	public static CommunicationClientImpl initChannelThread(
			SessionInfo sessionInfo) {
		CommunicationClientImpl communicationClient = CommunicationClientImpl
				.getCommunicationClient(sessionInfo);
		threadStart(communicationClient);
		return communicationClient;
	}

	/**
	 * @param communicationClient
	 *            线程启动
	 */
	public static void threadStart(CommunicationClientImpl communicationClient) {
		communicationClient.registConnection();
		Thread thread = new Thread(communicationClient);
		thread.start();
	}
}
