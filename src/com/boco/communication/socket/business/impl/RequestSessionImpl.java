package com.boco.communication.socket.business.impl;

import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import com.boco.communication.socket.bean.AdamEnum;
import com.boco.communication.socket.bean.SessionInfo;
import com.boco.communication.socket.business.RequestSessionIF;
import com.boco.communication.socket.intf.CommunicationClientIF;
import com.boco.communication.socket.intf.impl.CommunicationClientImpl;
import com.boco.communication.socket.util.CommunicationInfoUtil;

/**
 * @author LongJianYong
 * @date 2014-2-26 ����04:34:12
 * @version V1.0
 * @description �豸�ӿ�����ҵ����
 */
public class RequestSessionImpl implements RequestSessionIF {

	private Logger logger = Logger.getLogger(RequestSessionImpl.class);

	/* (non-Javadoc)
	 * @see ConmunicationIF#sendRequest(java.lang.String, int, byte[])
	 */
	public boolean send(CommunicationClientIF client, int type, byte[] data) {
		ByteBuffer buffer = CommunicationInfoUtil.getSendInfoBody(type, data);
		SessionInfo siBean = client.getSiBean();
		if (siBean == null)
			return false;
		if(type == AdamEnum.ADAM_HEAD_TYPE_ENUM.WEB_2_PROBER_REQUEST_VALUE)
			siBean.addSessionCount(); // ����������Ϊ��һ������ʱ������+1
		client.send(buffer);
		return true;
	}

	public boolean closeSession(SessionInfo siBean) {
		if (siBean == null)
			return false;
		logger.info("--ʣ��������:"+(siBean.getSessionCount()-1));
		if (siBean.getSessionCount() > 0) {
			// �رյ�ǰ������ʱ�жϵ�ǰͬһ�����ӵ�������������0ʱ��ʾ��������������ʹ�ô����ӣ�����1����
			siBean.delSessionCount();
//			����1֮�󻹴���0�Ļ���ֱ�ӷ���true��������Ҫ����ִ�к���Ĺر����Ӳ���
			if(siBean.getSessionCount() > 0)
				return true;
		}
		CommunicationClientIF client = CommunicationClientImpl.getCommunicationClient(siBean);
		CommunicationClientImpl.removeProberClient(siBean);
		return client.closeSessionSocket();
	}
}
