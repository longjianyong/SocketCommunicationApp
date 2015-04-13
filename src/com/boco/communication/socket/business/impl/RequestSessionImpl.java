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
 * @date 2014-2-26 下午04:34:12
 * @version V1.0
 * @description 设备接口请求业务处理
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
			siBean.addSessionCount(); // 当发送类型为第一次请求时连接数+1
		client.send(buffer);
		return true;
	}

	public boolean closeSession(SessionInfo siBean) {
		if (siBean == null)
			return false;
		logger.info("--剩余连接数:"+(siBean.getSessionCount()-1));
		if (siBean.getSessionCount() > 0) {
			// 关闭当前连接数时判断当前同一个连接的链接数，大于0时表示还有其他请求在使用此连接，做减1操作
			siBean.delSessionCount();
//			当减1之后还大于0的话就直接返回true，否则需要继续执行后面的关闭连接操作
			if(siBean.getSessionCount() > 0)
				return true;
		}
		CommunicationClientIF client = CommunicationClientImpl.getCommunicationClient(siBean);
		CommunicationClientImpl.removeProberClient(siBean);
		return client.closeSessionSocket();
	}
}
