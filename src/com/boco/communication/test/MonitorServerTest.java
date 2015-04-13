package com.boco.communication.test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import com.boco.communication.socket.util.CommunicationInfoUtil;
import com.boco.communication.socket.util.FormatTransferUtil;

/**
 * @author LongJianYong
 * @date 2014-2-26 下午03:21:10
 * @version V1.0
 * @description TODO
 */
public class MonitorServerTest implements Runnable {

	private ByteBuffer readBuffer = ByteBuffer.allocate(8);

	private Selector selector;

	private ServerSocketChannel ssc;

	public MonitorServerTest(InetAddress addr, int port) throws IOException {
		selector = Selector.open();
		ssc = ServerSocketChannel.open();
		ssc.configureBlocking(false);
		InetSocketAddress endpoint = new InetSocketAddress(addr, port);
		ssc.socket().bind(endpoint);
		ssc.register(selector, SelectionKey.OP_ACCEPT);
		System.out.println("启动成功！！！");
	}

	public static void main(String[] args) {
		try {
			MonitorServerTest mst = new MonitorServerTest(null, 8999);
			new Thread(mst).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		while (true) {
			try {
				this.selector.select();
			} catch (IOException e) {
				e.printStackTrace();
			}
			Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
			while (keys.hasNext()) {
				SelectionKey key = keys.next();
				keys.remove();

				if (!key.isValid()) {
					continue;
				}
				if (key.isReadable()) {
					read(key);
				} else if (key.isWritable()) {
					write(key);
				} else if (key.isAcceptable()) {
					accept(key);
				}
			}
		}
	}

	private void write(SelectionKey key) {

		System.out.println("响应客户端信息发送中....");
		SocketChannel sc = (SocketChannel) key.channel();
		String info = "my name is monitorServer,writing :hello world!";
		ByteBuffer writeBuffer = CommunicationInfoUtil
				.getSendInfoBody(3, info.getBytes());
		try {
			sc.write(writeBuffer);
			if (writeBuffer.remaining() > 0) {
				System.out.println("服务器信息没写完！！！");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		key.interestOps(SelectionKey.OP_READ);
	}

	private void read(SelectionKey key) {

		System.out.println("读取信息中...");
		SocketChannel sc = (SocketChannel) key.channel();
		this.readBuffer.clear();
		int lenInfo = -1;
		try {
			lenInfo = sc.read(this.readBuffer);
			if (lenInfo != -1) {
				int[] headInfo = CommunicationInfoUtil.getRsqInfoHead(this.readBuffer);
				int len = headInfo[0];
				System.out.println(len + "--------len-----type----------"
						+ headInfo[1]);
				len -= 8; // 数据包长度-包头长度 = 消息体长度
				byte[] rspData = new byte[len];
				ByteBuffer[] readBuffers = CommunicationInfoUtil.allocateByteBuffers(len,
						1024);
				long dataLen = sc.read(readBuffers);
				if (dataLen > 0) {
					for (int i = 0; i < readBuffers.length; i++) {
						System.arraycopy(readBuffers[i].array(), 0, rspData,
								i * 1024, readBuffers[i].limit());
					}
				}
				System.out.println("my name is monitorServer,reading :"
						+ FormatTransferUtil.bytesToString(rspData));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		key.interestOps(SelectionKey.OP_WRITE);
	}

	private void accept(SelectionKey key) {

		System.out.println("有链接过来了！！！");
		ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
		try {
			SocketChannel sc = serverChannel.accept();
			sc.configureBlocking(false);
			sc.register(this.selector, SelectionKey.OP_READ);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
