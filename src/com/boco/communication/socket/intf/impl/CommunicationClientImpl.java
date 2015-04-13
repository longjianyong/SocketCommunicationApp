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
 * @date 2014-2-26 下午02:55:28
 * @version V1.0
 * @description TODO
 */
public class CommunicationClientImpl implements CommunicationClientIF, Runnable {

	private Logger logger = Logger.getLogger(CommunicationClientImpl.class);

	// 选择注册器
	private Selector selector = null;
	// 当前线程使用的socket通道对象
	private SocketChannel socketChannel = null;
//	当前socket对象的链接信息
	private SessionInfo siBean = null;
	// 读取消息体数据时设备bytebuffer的默认大小
	private final int READ_BUFFER_LEN = 1024;
	// 读取消息包头长度
	private ByteBuffer readHeadBuffer = ByteBuffer.allocate(8);
	// 写入缓存区
	private ByteBuffer writeBuffer = ByteBuffer.allocate(8192);
	// 信息发送队列缓存
	private Queue<ByteBuffer> sendQueue = new LinkedList<ByteBuffer>();
	// 公用通信事件集合
	private List<RegisteEvent> changes = new LinkedList<RegisteEvent>();

	// 针对于自定义事件需要在changes同步代码块外面执行，需要集合单独存储
	private List<RegisteEvent> mySelfEvent = new ArrayList<RegisteEvent>();

	// 是否阻塞标识
	private boolean isBlocking = false;

	private static Map<String, CommunicationClientImpl> serverBeanMap = new HashMap<String, CommunicationClientImpl>();
	
	/**
	 * 私有构造函数
	 */
	private CommunicationClientImpl() {

	}
	
	/**
	 * @param sBean
	 * @return	相同目标时获取单例对象
	 */
	public static CommunicationClientImpl getCommunicationClient(SessionInfo sBean){
		CommunicationClientImpl pClient = serverBeanMap.get(sBean.getHostName()+sBean.getPort());
		if(pClient == null){
			synchronized (serverBeanMap) {
//				此处又加了一个判断，是防止多线程时pClient变换问题
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
	 * 删除链接对象
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
	 * @return 通信通道的selector选择器
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
//		当发送数据不为空时入队，如果为null只注册发送事件把queue队列中的数据发送完
		if(buffer != null)
			this.registerSendQueue(buffer); // 注册发送数据
		if (this.socketChannel.isConnected()) { // 链接没有断开
			// 注册自定义的发送事件
			this.addChangesEvent(RegisteEvent.SELECTOR_EVENT,
					RegisteEvent.EVE_SEND_EVENT);
		} else if (siBean.getReconnectCount() > 1) { // 链接断开，如果已经开始重连则不再注册重连事件
			// 往链接重连后需要注册的事件集合中添加事件
			this.insertConnectedEvent(RegisteEvent.SELECTOR_EVENT,
					RegisteEvent.EVE_SEND_EVENT);
		} else { // 链接断开，还没有开始重连
			// 重连后注册自定义的发送事件
			this.insertConnectedEvent(RegisteEvent.SELECTOR_EVENT,
					RegisteEvent.EVE_SEND_EVENT);
			// 注册自定义的链接超时事件，进行重连
			this.addChangesEvent(RegisteEvent.SELECTOR_EVENT,
					RegisteEvent.EVE_TIMEOUT_EVENT);
		}
		// 判断是否阻塞
		if (this.isBlocking)
			this.selector.wakeup();
	}

	/**
	 * @param socketChannel
	 * @return 关闭socket链接
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
	 *            ：发送请求消息对象，用来存储socket、server、response对象
	 * @param type
	 *            ：发送的消息类型
	 * @param data
	 *            ：消息体
	 * @throws IOException
	 */
	private void registerSendQueue(ByteBuffer buffer) {
		synchronized (this.sendQueue) {
			// 把数据存入发送队列中
			sendQueue.offer(buffer);
		}
	}

	/**
	 * @param siBean
	 *            ：发送请求消息对象，用来存储socket、server、response对象
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

			// 插入链接建立后需要注册的事件
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
	 * 插入链接建立后的处理事件
	 */
	private void insertConnectedEvent(int type, int event) {
		// 如果事件已经存在，则直接返回
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

				// 当没有对应于注册的key的事件发生时将回调（不使用select阻塞，避免可读事件的阻塞影响可写事件的执行）
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
	 *            建立链接或重连时完成链接的方法
	 */
	private void finishedConnection(SelectionKey key) {
		SocketChannel sc = (SocketChannel) key.channel();
//		SocketInfo siBean = (SocketInfo) key.attachment();
		try {
			int i = 0;
			while (!sc.finishConnect()) {
				i++;
				if (i >= 60) {
					// 当前连接无法完成时重新注册链接
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
			// 重连后注册发送数据，并判断是否是again请求
//			verifyAgainResponse(siBean);
			siBean.setReconnectCount(0); // 当重连成功后标识重置为0
			System.out.println("连接Prober服务器成功！！！");
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
	 * 满足一定业务的重连时将进行again请求
	 */
/*	private void verifyAgainResponse(SendInfoBean siBean) {

		ByteBuffer bufferData = null;
		// 满足此类型的重连将进行again请求数据封装
		if (siBean.requestType == AdamEnum.ADAM_HEAD_TYPE_ENUM.WEB_2_PROBER_REQUEST_VERIFY_VALUE)
			try {
				bufferData = CommunicationInfoUtil.getSendInfoBody(
						siBean.requestType, DeviceProtoUtil.transferBuffer(
								siBean.requestType, siBean.requestData));
			} catch (InvalidProtocolBufferException e) {
				logger.error("again请求数据封装异常！", e);
			}
		else { // 不满足的情况下重新发送原始数据
			bufferData = CommunicationInfoUtil.getSendInfoBody(
					siBean.requestType, siBean.requestData);
		}
		registerSendQueue(siBean, bufferData);
	}*/

	/**
	 * @param key
	 * 
	 *            注册可读事件触发的读取方法
	 * 
	 */
	private void read(SelectionKey key) {
		SocketChannel sc = (SocketChannel) key.channel();
//		SocketInfo siBean = (SocketInfo) key.attachment();
		int headLen = -1;
		try {
			String tName = Thread.currentThread().getName();
			System.out.println(tName+"读取Prober响应数据.....");
			this.readHeadBuffer.clear();
			headLen = sc.read(this.readHeadBuffer); // 需要处理数据头信息后继续读取
			if (headLen != -1) {
				System.out.println("read head lenght: "+headLen);
				int[] headInfo = CommunicationInfoUtil
						.getRsqInfoHead(this.readHeadBuffer);
				int len = headInfo[0];
				System.out.println("total lenght: "+len);
				len -= 8; // 数据包长度-包头长度 = 消息体长度
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
							Thread.sleep(200);	//当接收长度为0时,等待数据接收
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
				// 服务器链接关闭后触发客户端的可读事件，但是channel中的数据为空，此时需要重连
				// if (!siBean.getSocket().isConnected()) {
				addChangesEvent(RegisteEvent.SELECTOR_EVENT,
						RegisteEvent.EVE_TIMEOUT_EVENT);
				// }
			}
			System.out.println(tName+"读取Prober响应数据.....完成!");
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
//		// 1、是否需要設置多久沒有響應就直接關閉連接
//		// 2、響應結束標識來判斷關閉連接
//		// 3、由业务层来控制是否关闭连接
		if (resHandler.handlerResponse(this.siBean.getHandlerClass(), type, data)) {
//
		}
	}

	/**
	 * @param key
	 * 
	 *            当socket缓存区已满时注册可写事件触发的写方法
	 */
	private void write(SelectionKey key) {
		SocketChannel sc = (SocketChannel) key.channel();
//		SocketInfo siBean = (SocketInfo) key.attachment();
		if (this.writeBuffer.hasRemaining()) {
			System.out.println("开始向Prober发送请求.....");
			ByteBuffer buf = this.writeBuffer;
			try {
				sc.write(buf);
				long len = -1;
				while(buf.hasRemaining()){
					len = buf.remaining();
					sc.write(buf);
					if(len == buf.remaining() && len > 0){
						System.out.println("socket channel's cache was full..., remaining len："
								+ len + "，waiting write....");
						// 表示buf的内容没有写完，或者是socketChannel中的buffer已经满了
						this.writeBuffer = buf;
						buf = null;	//不能使用clear，clear仅仅只是初始化当前位置为0而已
						// 注册可写事件
						if(this.isBlocking)
							this.selector.wakeup();
						return;
					}
				}
//				当前请求的数据发送成功之后更新发送状态，以便于业务层做相应处理
				synchronized (this.sendQueue) {
					if (!this.sendQueue.isEmpty()) {
						// 当写缓存区的数据发送完之后判断发送队列还有数据，触发自定义发送事件
						addChangesEvent(RegisteEvent.SELECTOR_EVENT,
								RegisteEvent.EVE_SEND_EVENT);
						// 修改注册的可写事件为可读事件
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
	 * 当sendQueue有发送数据时触发的写方法
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
							System.out.println("socket channel's cache was full..., remaining len："
									+ len + "，waiting write....");
							// 表示buf的内容没有写完，或者是socketChannel中的buffer已经满了
							this.writeBuffer = buf;
							// 注册可写事件
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
	 *            ：发送请求消息对象，用来存储socket、server、response对象
	 * @param eType
	 *            ：事件类型（註冊通道、修改selector的註冊事件、連接超時）
	 * @param eOps
	 *            ：当前变更请求的事件key
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
	 *            链接重连的事件
	 */
	private void addChangesEvent(RegisteEvent eventObj) {
		synchronized (changes) {
			changes.add(eventObj);
		}
	}

	/**
	 * 重连方法： 判断是否到达最大重连次数，
	 * 
	 * @throws IOException
	 * 
	 */
	public void reconnect(RegisteEvent change) {

		SessionInfo siBean = (SessionInfo) change.eventObj;

		if (siBean.getSessionCount() == 0 && siBean.getSessionType() != 1) {		//当连接数都为0且不是长链接的时候就不需要再进行重连
			if (this.socketChannel != null)
				closeSessionSocket();
			System.out.println("停止了一个链接的重连操作！");
			return;
		}
		System.out.println("请求链接第 " + siBean.getReconnectCount() + "次重连！");
		// 重连之前关闭socketChannel
		closeSessionSocket();

		int sleepTime = 3 * 1000 * siBean.getReconnectCount(); // 每三秒重连一次
		sleepTime = sleepTime >= 300 * 1000 ? 300 * 1000 : sleepTime;
		siBean.addReconnectCount();
		try {
			Thread.sleep(sleepTime);
			// 链接断开后重连
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
