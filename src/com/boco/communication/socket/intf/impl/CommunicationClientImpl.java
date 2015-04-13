package com.boco.communication.socket.intf.impl;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.apache.log4j.Logger;

import com.boco.communication.socket.bean.SessionInfo;
import com.boco.communication.socket.business.ResponseSessionAbstract;
import com.boco.communication.socket.business.impl.ResponseSessionAbstractImpl;
import com.boco.communication.socket.intf.CommunicationClientIF;
import com.boco.communication.socket.util.CommunicationInfoUtil;
import com.boco.communication.socket.util.FormatTransferUtil;
import com.boco.communication.socket.util.RegisteEvent;

/**
 * @author LongJianYong
 * @date 2014-2-26 ����02:55:28
 * @version V1.0
 * @description TODO
 */
public class CommunicationClientImpl implements CommunicationClientIF, Runnable {

	private Logger logger = Logger.getLogger(CommunicationClientImpl.class);

	// ѡ��ע����
	private Selector selector = null;
	// ��ǰ�߳�ʹ�õ�socketͨ������
	private SocketChannel socketChannel = null;
//	��ǰsocket�����������Ϣ
	private SessionInfo siBean = null;
	// ��ȡ��Ϣ������ʱ�豸bytebuffer��Ĭ�ϴ�С
	private final int READ_BUFFER_LEN = 1024;
	// ��ȡ��Ϣ��ͷ����
	private ByteBuffer readHeadBuffer = ByteBuffer.allocate(8);
	// д�뻺����
	private ByteBuffer writeBuffer = ByteBuffer.allocate(8192);
	// ��Ϣ���Ͷ��л���
	private Queue<ByteBuffer> sendQueue = new LinkedList<ByteBuffer>();
	// ����ͨ���¼�����
	private List<RegisteEvent> changes = new LinkedList<RegisteEvent>();

	// ������Զ����¼���Ҫ��changesͬ�����������ִ�У���Ҫ���ϵ����洢
	private List<RegisteEvent> mySelfEvent = new ArrayList<RegisteEvent>();

	// �Ƿ�������ʶ
	private boolean isBlocking = false;

	private static Map<String, CommunicationClientImpl> serverBeanMap = new HashMap<String, CommunicationClientImpl>();
	
	/**
	 * ˽�й��캯��
	 */
	private CommunicationClientImpl() {

	}
	
	/**
	 * @param sBean
	 * @return	��ͬĿ��ʱ��ȡ��������
	 */
	public static CommunicationClientImpl getCommunicationClient(SessionInfo sBean){
		CommunicationClientImpl pClient = serverBeanMap.get(sBean.getHostName()+sBean.getPort());
		if(pClient == null){
			synchronized (serverBeanMap) {
//				�˴��ּ���һ���жϣ��Ƿ�ֹ���߳�ʱpClient�任����
				if(pClient == null){
					pClient = new CommunicationClientImpl();
					pClient.siBean = sBean;
					serverBeanMap.put(sBean.getHostName()+sBean.getPort(), pClient);
				}
			}
		}
		return pClient;
	}
	
	/**
	 * @param sBean
	 * ɾ�����Ӷ���
	 */
	public static void removeProberClient(SessionInfo sBean){
		CommunicationClientImpl pClient = serverBeanMap.get(sBean.getHostName()+sBean.getPort());
		if(pClient == null){
			synchronized (serverBeanMap) {
				if(pClient == null){
					serverBeanMap.remove(sBean.getHostName()+sBean.getPort());
				}
			}
		}
	}
	
	public static synchronized  Map<String, CommunicationClientImpl> getServerBeanMap(){
		return serverBeanMap;
	}
	
	
	/**
	 * @return ͨ��ͨ����selectorѡ����
	 * @throws IOException
	 */
	private Selector initSelector() throws IOException {
		if (this.selector == null)
			// return Selector.open();
			return SelectorProvider.provider().openSelector();
		else
			return this.selector;
	}
	
	public void send(ByteBuffer buffer) {
//		���������ݲ�Ϊ��ʱ��ӣ����Ϊnullֻע�ᷢ���¼���queue�����е����ݷ�����
		if(buffer != null)
			this.registerSendQueue(buffer); // ע�ᷢ������
		if (this.socketChannel.isConnected()) { // ����û�жϿ�
			// ע���Զ���ķ����¼�
			this.addChangesEvent(RegisteEvent.SELECTOR_EVENT,
					RegisteEvent.EVE_SEND_EVENT);
		} else if (siBean.getReconnectCount() > 1) { // ���ӶϿ�������Ѿ���ʼ��������ע�������¼�
			// ��������������Ҫע����¼�����������¼�
			this.insertConnectedEvent(RegisteEvent.SELECTOR_EVENT,
					RegisteEvent.EVE_SEND_EVENT);
		} else { // ���ӶϿ�����û�п�ʼ����
			// ������ע���Զ���ķ����¼�
			this.insertConnectedEvent(RegisteEvent.SELECTOR_EVENT,
					RegisteEvent.EVE_SEND_EVENT);
			// ע���Զ�������ӳ�ʱ�¼�����������
			this.addChangesEvent(RegisteEvent.SELECTOR_EVENT,
					RegisteEvent.EVE_TIMEOUT_EVENT);
		}
		// �ж��Ƿ�����
		if (this.isBlocking)
			this.selector.wakeup();
	}

	/**
	 * @param socketChannel
	 * @return �ر�socket����
	 */
	public boolean closeSessionSocket() {
		try {
			if(this.socketChannel == null)
				return true;
			SelectionKey keys = this.socketChannel.keyFor(this.selector);
			if (keys != null)
				keys.cancel();
			if (this.socketChannel.isOpen())
				this.socketChannel.close();
			this.socketChannel = null;
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * @param siBean
	 *            ������������Ϣ���������洢socket��server��response����
	 * @param type
	 *            �����͵���Ϣ����
	 * @param data
	 *            ����Ϣ��
	 * @throws IOException
	 */
	private void registerSendQueue(ByteBuffer buffer) {
		synchronized (this.sendQueue) {
			// �����ݴ��뷢�Ͷ�����
			sendQueue.offer(buffer);
		}
	}

	/**
	 * @param siBean
	 *            ������������Ϣ���������洢socket��server��response����
	 * @return
	 * @throws IOException
	 */
	public void registConnection() {
		try {
			if (this.socketChannel == null) {
				this.socketChannel = SocketChannel.open();
				this.socketChannel.configureBlocking(false);
			}
			if (this.socketChannel.isConnected())
				return ;
			this.socketChannel.connect(new InetSocketAddress(siBean.getHostName(),
					siBean.getPort()));

			// �������ӽ�������Ҫע����¼�
			insertConnectedEvent(RegisteEvent.SELECTOR_CHANGE_OPS,
					SelectionKey.OP_READ);

			addChangesEvent(RegisteEvent.SELECTOR_REGISTER_CHANNEL,
					SelectionKey.OP_CONNECT);
			if (siBean.getReconnectCount() > 0)
				this.selector.wakeup();
			this.selector = initSelector();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param sBean
	 * @param type
	 * @param event
	 * �������ӽ�����Ĵ����¼�
	 */
	private void insertConnectedEvent(int type, int event) {
		// ����¼��Ѿ����ڣ���ֱ�ӷ���
		if (siBean.connectedEventMap.get(event) != null)
			return;
		RegisteEvent reEvent = new RegisteEvent(this.socketChannel, type, event);
		siBean.connectedEventMap.put(event, reEvent);
	}


	@Override
	public void run() {
		while (true) {
			try {
				synchronized (changes) {
					if (!changes.isEmpty()) {
						Iterator<RegisteEvent> cs = changes.iterator();
						while (cs.hasNext()) {
							RegisteEvent change = cs.next();
//							SessionInfo sib = (SessionInfo) change.eventObj;
							switch (change.type) {
							case RegisteEvent.SELECTOR_CHANGE_OPS:
								SelectionKey key = this.socketChannel.keyFor(
										this.selector);
								key.interestOps(change.ops);
//								key.attach(sib);
								break;
							case RegisteEvent.SELECTOR_REGISTER_CHANNEL:
								this.socketChannel.register(
										this.selector, change.ops);
//								k.attach(sib);
								break;
							case RegisteEvent.SELECTOR_EVENT:
								mySelfEvent.add(change);
								break;
							}
						}
						changes.clear();
					}
				}
				if (!mySelfEvent.isEmpty()) {
					for (RegisteEvent myEvent : mySelfEvent) {
						eventProcess(myEvent);
					}
					mySelfEvent.clear();
				}

				// ��û�ж�Ӧ��ע���key���¼�����ʱ���ص�����ʹ��select����������ɶ��¼�������Ӱ���д�¼���ִ�У�
				// if (this.selector.selectNow() == 0)
				// continue;
				this.isBlocking = true;
				this.selector.select();
				this.isBlocking = false;
				Iterator<SelectionKey> keys = this.selector.selectedKeys()
						.iterator();
				while (keys.hasNext()) {
					SelectionKey key = keys.next();
					keys.remove();
					if (!key.isValid())
						continue;
					switch (key.readyOps()) {
					case SelectionKey.OP_READ:
						read(key);
						break;
					case SelectionKey.OP_WRITE:
						write(key);
						break;
					case SelectionKey.OP_CONNECT:
						finishedConnection(key);
						break;
					}
				}
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	/**
	 * @param key
	 * 
	 *            �������ӻ�����ʱ������ӵķ���
	 */
	private void finishedConnection(SelectionKey key) {
		SocketChannel sc = (SocketChannel) key.channel();
//		SocketInfo siBean = (SocketInfo) key.attachment();
		try {
			int i = 0;
			while (!sc.finishConnect()) {
				i++;
				if (i >= 60) {
					// ��ǰ�����޷����ʱ����ע������
					addChangesEvent(RegisteEvent.SELECTOR_EVENT,
							RegisteEvent.EVE_TIMEOUT_EVENT);
					return;
				}
				Thread.sleep(50);
			}
			if (!siBean.connectedEventMap.isEmpty()) {
				List<RegisteEvent> eventList = new ArrayList<RegisteEvent>(
						siBean.connectedEventMap.values());
				for (RegisteEvent registeEvent : eventList) {
					registeEvent.eventObj = siBean;
					addChangesEvent(registeEvent);
				}
				siBean.connectedEventMap.clear();
			}
			// ������ע�ᷢ�����ݣ����ж��Ƿ���again����
//			verifyAgainResponse(siBean);
			siBean.setReconnectCount(0); // �������ɹ����ʶ����Ϊ0
			System.out.println("����Prober�������ɹ�������");
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			if (!sc.isConnected()) {
				addChangesEvent(RegisteEvent.SELECTOR_EVENT,
						RegisteEvent.EVE_TIMEOUT_EVENT);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param siBean
	 * ����һ��ҵ�������ʱ������again����
	 */
/*	private void verifyAgainResponse(SendInfoBean siBean) {

		ByteBuffer bufferData = null;
		// ��������͵�����������again�������ݷ�װ
		if (siBean.requestType == AdamEnum.ADAM_HEAD_TYPE_ENUM.WEB_2_PROBER_REQUEST_VERIFY_VALUE)
			try {
				bufferData = CommunicationInfoUtil.getSendInfoBody(
						siBean.requestType, DeviceProtoUtil.transferBuffer(
								siBean.requestType, siBean.requestData));
			} catch (InvalidProtocolBufferException e) {
				logger.error("again�������ݷ�װ�쳣��", e);
			}
		else { // ���������������·���ԭʼ����
			bufferData = CommunicationInfoUtil.getSendInfoBody(
					siBean.requestType, siBean.requestData);
		}
		registerSendQueue(siBean, bufferData);
	}*/

	/**
	 * @param key
	 * 
	 *            ע��ɶ��¼������Ķ�ȡ����
	 * 
	 */
	private void read(SelectionKey key) {
		SocketChannel sc = (SocketChannel) key.channel();
//		SocketInfo siBean = (SocketInfo) key.attachment();
		int headLen = -1;
		try {
			String tName = Thread.currentThread().getName();
			System.out.println(tName+"��ȡProber��Ӧ����.....");
			this.readHeadBuffer.clear();
			headLen = sc.read(this.readHeadBuffer); // ��Ҫ��������ͷ��Ϣ�������ȡ
			if (headLen != -1) {
				System.out.println("read head lenght: "+headLen);
				int[] headInfo = CommunicationInfoUtil
						.getRsqInfoHead(this.readHeadBuffer);
				int len = headInfo[0];
				System.out.println("total lenght: "+len);
				len -= 8; // ���ݰ�����-��ͷ���� = ��Ϣ�峤��
				byte[] rspData = new byte[len];
				ByteBuffer readBuffers = ByteBuffer.allocate(READ_BUFFER_LEN);
				long time = 0l;
				int dataLen = 0;
				int j = 0;
				while(len > 0)
				{
					len -= dataLen;
					j += dataLen;
					readBuffers.clear();
					if(len < READ_BUFFER_LEN)
						readBuffers = ByteBuffer.allocate(len);
					dataLen = sc.read(readBuffers);
					if(dataLen == 0){
						if(time == 0l)
							time = System.currentTimeMillis();
						if(System.currentTimeMillis() - time > 60000)
							break;
						try {
							Thread.sleep(200);	//�����ճ���Ϊ0ʱ,�ȴ����ݽ���
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}else{
						time = 0l;
					}
//					System.out.println("read lenght:" +dataLen);
					if(readBuffers.hasArray())
						System.arraycopy(readBuffers.array(), 0, rspData, j, dataLen);
				}
				readBuffers = null;
				System.out.println("readed lenght:" +rspData.length);
				System.out.println("readed lenght:" +FormatTransferUtil.bytesToString(rspData));
				this.handleResponse(headInfo[1], rspData);
			} else {
				// ���������ӹرպ󴥷��ͻ��˵Ŀɶ��¼�������channel�е�����Ϊ�գ���ʱ��Ҫ����
				// if (!siBean.getSocket().isConnected()) {
				addChangesEvent(RegisteEvent.SELECTOR_EVENT,
						RegisteEvent.EVE_TIMEOUT_EVENT);
				// }
			}
			System.out.println(tName+"��ȡProber��Ӧ����.....���!");
		} catch (IOException e) {
			logger.error("sc.isConnected():"+sc.isConnected()+","+e.getMessage(), e);
			addChangesEvent(RegisteEvent.SELECTOR_EVENT,
					RegisteEvent.EVE_TIMEOUT_EVENT);
		}
	}

	/**
	 * @param socketChannel
	 * @param type
	 * @param data
	 * @throws IOException
	 */
	public void handleResponse(int type, byte[] data)
			throws IOException {

		ResponseSessionAbstract resHandler = ResponseSessionAbstractImpl.getResponseHandlerImpl();
//		// 1���Ƿ���Ҫ�O�ö�Û]��푑���ֱ���P�]�B��
//		// 2��푑��Y�����R���Д��P�]�B��
//		// 3����ҵ����������Ƿ�ر�����
		if (resHandler.handlerResponse(this.siBean.getHandlerClass(), type, data)) {
//
		}
	}

	/**
	 * @param key
	 * 
	 *            ��socket����������ʱע���д�¼�������д����
	 */
	private void write(SelectionKey key) {
		SocketChannel sc = (SocketChannel) key.channel();
//		SocketInfo siBean = (SocketInfo) key.attachment();
		if (this.writeBuffer.hasRemaining()) {
			System.out.println("��ʼ��Prober��������.....");
			ByteBuffer buf = this.writeBuffer;
			try {
				sc.write(buf);
				long len = -1;
				while(buf.hasRemaining()){
					len = buf.remaining();
					sc.write(buf);
					if(len == buf.remaining() && len > 0){
						System.out.println("socket channel's cache was full..., remaining len��"
								+ len + "��waiting write....");
						// ��ʾbuf������û��д�꣬������socketChannel�е�buffer�Ѿ�����
						this.writeBuffer = buf;
						buf = null;	//����ʹ��clear��clear����ֻ�ǳ�ʼ����ǰλ��Ϊ0����
						// ע���д�¼�
						if(this.isBlocking)
							this.selector.wakeup();
						return;
					}
				}
//				��ǰ��������ݷ��ͳɹ�֮����·���״̬���Ա���ҵ�������Ӧ����
				synchronized (this.sendQueue) {
					if (!this.sendQueue.isEmpty()) {
						// ��д�����������ݷ�����֮���жϷ��Ͷ��л������ݣ������Զ��巢���¼�
						addChangesEvent(RegisteEvent.SELECTOR_EVENT,
								RegisteEvent.EVE_SEND_EVENT);
						// �޸�ע��Ŀ�д�¼�Ϊ�ɶ��¼�
						addChangesEvent(
								RegisteEvent.SELECTOR_CHANGE_OPS,
								SelectionKey.OP_READ);
					} else {
						addChangesEvent(
								RegisteEvent.SELECTOR_CHANGE_OPS,
								SelectionKey.OP_READ);
					}
				}
			} catch (IOException e) {
				logger.error("sc.isConnected():"+sc.isConnected()+","+e.getMessage(), e);
				if (!sc.isConnected()) {
					insertConnectedEvent(
							RegisteEvent.SELECTOR_CHANGE_OPS,
							SelectionKey.OP_WRITE);
					addChangesEvent(RegisteEvent.SELECTOR_EVENT,
							RegisteEvent.EVE_TIMEOUT_EVENT);
				}
			}
		}

	}

	/**
	 * ��sendQueue�з�������ʱ������д����
	 */
	private void write(RegisteEvent change) {
//		SocketInfo siBean = (SocketInfo) change.eventObj;
		try {
			ByteBuffer buf;
			synchronized (this.sendQueue) {
				send:while (!this.sendQueue.isEmpty() && this.socketChannel.isConnected()) {
					buf = this.sendQueue.poll();
					this.socketChannel.write(buf);
					long len = -1;
					while(buf.hasRemaining()){
						len = buf.remaining();
						this.socketChannel.write(buf);
						if(len == buf.remaining() && len > 0){
							System.out.println("socket channel's cache was full..., remaining len��"
									+ len + "��waiting write....");
							// ��ʾbuf������û��д�꣬������socketChannel�е�buffer�Ѿ�����
							this.writeBuffer = buf;
							// ע���д�¼�
							addChangesEvent(RegisteEvent.SELECTOR_CHANGE_OPS,SelectionKey.OP_WRITE);
							buf = null;
							break send;
						}
					}
					buf = null;
				}
			}
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			if (!this.socketChannel.isConnected()) {
				insertConnectedEvent(RegisteEvent.SELECTOR_EVENT,
						RegisteEvent.EVE_SEND_EVENT);
				addChangesEvent(RegisteEvent.SELECTOR_EVENT,
						RegisteEvent.EVE_TIMEOUT_EVENT);
			}
		}
	}

	/**
	 * @param siBean
	 *            ������������Ϣ���������洢socket��server��response����
	 * @param eType
	 *            ���¼����ͣ��]��ͨ�����޸�selector���]���¼����B�ӳ��r��
	 * @param eOps
	 *            ����ǰ���������¼�key
	 */
	private void addChangesEvent(int eType, int eOps) {
		synchronized (changes) {
			changes.add(new RegisteEvent(siBean, eType, eOps));
			if(this.isBlocking && eType == RegisteEvent.SELECTOR_CHANGE_OPS)
				this.selector.wakeup();
		}
	}

	/**
	 * @param eventObj
	 *            �����������¼�
	 */
	private void addChangesEvent(RegisteEvent eventObj) {
		synchronized (changes) {
			changes.add(eventObj);
		}
	}

	/**
	 * ���������� �ж��Ƿ񵽴��������������
	 * 
	 * @throws IOException
	 * 
	 */
	public void reconnect(RegisteEvent change) {

		SessionInfo siBean = (SessionInfo) change.eventObj;

		if (siBean.getSessionCount() == 0 && siBean.getSessionType() != 1) {		//����������Ϊ0�Ҳ��ǳ����ӵ�ʱ��Ͳ���Ҫ�ٽ�������
			if (this.socketChannel != null)
				closeSessionSocket();
			System.out.println("ֹͣ��һ�����ӵ�����������");
			return;
		}
		System.out.println("�������ӵ� " + siBean.getReconnectCount() + "��������");
		// ����֮ǰ�ر�socketChannel
		closeSessionSocket();

		int sleepTime = 3 * 1000 * siBean.getReconnectCount(); // ÿ��������һ��
		sleepTime = sleepTime >= 300 * 1000 ? 300 * 1000 : sleepTime;
		siBean.addReconnectCount();
		try {
			Thread.sleep(sleepTime);
			// ���ӶϿ�������
			registConnection();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param change
	 */
	private void eventProcess(RegisteEvent change) {
		RegisteEvent cr = change;
		switch (cr.ops) {
		case RegisteEvent.EVE_SEND_EVENT:
			write(cr);
			break;
		case RegisteEvent.EVE_TIMEOUT_EVENT:
			reconnect(cr);
			break;
		}
	}
	
	public boolean isConnected() {
		if(this.socketChannel != null && this.socketChannel.isConnected())
		{
			return true;
		}
		return false;
	}

	public SessionInfo getSiBean() {
		return siBean;
	}

	public void setSiBean(SessionInfo siBean) {
		this.siBean = siBean;
	}
	
}
