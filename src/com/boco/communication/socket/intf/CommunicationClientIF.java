package com.boco.communication.socket.intf;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.boco.communication.socket.bean.SessionInfo;
import com.boco.communication.socket.util.RegisteEvent;

/**
 * @author longjianyong 网络通信接口
 */
public interface CommunicationClientIF {

	/**
	 * 测试链接是否正常
	 * @return
	 */
	public boolean isConnected();
	/**
	 *            注册链接
	 */
	public void registConnection();

	/**
	 * @param buffer
	 *            请求发送
	 */
	public void send(ByteBuffer buffer);

	/**
	 * @param type
	 * @param data
	 *            响应结果处理
	 */
	public void handleResponse(int type, byte[] data)
			throws IOException;

	/**
	 * @param change
	 *            链接重连
	 */
	public void reconnect(RegisteEvent change);

	/**
	 * @return 关闭链接
	 */
	public boolean closeSessionSocket();
	
	/**
	 * @return 获取当前socket链接信息
	 */
	public SessionInfo getSiBean();
}
