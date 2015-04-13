package com.boco.communication.invoke.util;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.boco.communication.socket.bean.SessionInfo;
import com.boco.communication.socket.intf.CommunicationClientIF;
import com.boco.communication.socket.intf.impl.CommunicationClientImpl;

/**
 * @author LongJianYong 1����ʼ������ͨ���߳� 2�����ؾ����ʵ��
 */
public class LoadBalanceUtil {

	/**
	 * @param sInfo
	 * @return ���ؾ���ͻ���ͨ��
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
	 * @return �򵥵ĸ��ؾ��⣬�������������ٵĿͻ��˶���
	 */
	private static CommunicationClientIF getLoadBalanceClient(
			Collection<CommunicationClientImpl> socketList) {
		CommunicationClientIF info = null;
		for (CommunicationClientIF client : socketList) {
			if (!client.isConnected()) // �����ǰ������Ч������
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
	 *            ��ʼ��������Ҫ������ͨ���߳�
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
	 *            �߳�����
	 */
	public static void threadStart(CommunicationClientImpl communicationClient) {
		communicationClient.registConnection();
		Thread thread = new Thread(communicationClient);
		thread.start();
	}
}
