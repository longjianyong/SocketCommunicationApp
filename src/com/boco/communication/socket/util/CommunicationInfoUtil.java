package com.boco.communication.socket.util;



import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

/**
 * @author LongJianYong
 * @date 2014-3-3 下午03:52:16   
 * @version V1.0 
 * @description socket通信时使用的数据封装和转换
 */
public class CommunicationInfoUtil {

	private static Logger log = Logger.getLogger(CommunicationInfoUtil.class);
	/**
	 * @param infoLen
	 *            消息体长度
	 * @param byteBufferLen
	 *            每个ByteBuffer对象的长度
	 * @return 读取数据用的ByteBuffer数组
	 */
	public static ByteBuffer[] allocateByteBuffers(int infoLen,
			int byteBufferLen) {
		int groups;
		ByteBuffer[] readBuffers;
		if (infoLen % byteBufferLen == 0) {
			groups = infoLen / byteBufferLen;
			readBuffers = new ByteBuffer[groups];
			for (int i = 0; i < groups; i++) {
				readBuffers[i] = ByteBuffer.allocate(byteBufferLen);
			}
		} else {
			groups = infoLen / byteBufferLen + 1;
			readBuffers = new ByteBuffer[groups];
			for (int i = 0; i < groups - 1; i++) {
				readBuffers[i] = ByteBuffer.allocate(byteBufferLen);
			}
			readBuffers[groups - 1] = ByteBuffer.allocate(infoLen
					% byteBufferLen);
		}
		return readBuffers;
	}
	
	/**
	 * @param type  	请求操作类型
	 * @param info		请求发送的消息
	 * @return			返回封装好的发送消息体
	 */
	public static ByteBuffer getSendInfoBody(int type,byte[] info){
		int len = 8+info.length;
//		整形转换成网络字节序
		byte[] typeByte = FormatTransferUtil.toHH(type);
		byte[] lenByte = FormatTransferUtil.toHH(len);
		log.debug("send data type:"+type+", len:"+len);
		byte[] sendInfo = new byte[len];
//		消息体的组成：len + type + info
		System.arraycopy(lenByte, 0, sendInfo, 0, lenByte.length);
		System.arraycopy(typeByte, 0, sendInfo, typeByte.length,
				typeByte.length);
		System.arraycopy(info, 0, sendInfo, lenByte.length + typeByte.length,
				info.length);
		log.info("the send ByteBuffer size="+sendInfo.length);
		return ByteBuffer.wrap(sendInfo);
	}
		
	/**
	 * @param byteBuffer	服务端响应返回的消息头对象
	 * @return				转换网络字节序，获取响应消息体的长度、响应类型
	 */
	public static int[] getRsqInfoHead(ByteBuffer byteBuffer){
		int[] head = new int[2];
		byte[] bt = new byte[4];
		byteBuffer.position(0);		//重置缓存的当前位置为0
		byteBuffer.get(bt);
//		网络字节序转换成整形
		head[0] = FormatTransferUtil.hBytesToInt(bt);
		byteBuffer.get(bt);
		head[1] = FormatTransferUtil.hBytesToInt(bt);
		log.debug(byteBuffer.order().toString()+" receive data type:"+head[1]+", len:"+head[0]);
		return head;
	}
	
}
