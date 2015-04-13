package com.boco.communication.socket.intf;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.boco.communication.socket.bean.SessionInfo;
import com.boco.communication.socket.util.RegisteEvent;

/**
 * @author longjianyong ����ͨ�Žӿ�
 */
public interface CommunicationClientIF {

	/**
	 * ���������Ƿ�����
	 * @return
	 */
	public boolean isConnected();
	/**
	 *            ע������
	 */
	public void registConnection();

	/**
	 * @param buffer
	 *            ������
	 */
	public void send(ByteBuffer buffer);

	/**
	 * @param type
	 * @param data
	 *            ��Ӧ�������
	 */
	public void handleResponse(int type, byte[] data)
			throws IOException;

	/**
	 * @param change
	 *            ��������
	 */
	public void reconnect(RegisteEvent change);

	/**
	 * @return �ر�����
	 */
	public boolean closeSessionSocket();
	
	/**
	 * @return ��ȡ��ǰsocket������Ϣ
	 */
	public SessionInfo getSiBean();
}
