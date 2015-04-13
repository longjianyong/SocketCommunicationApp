package com.boco.communication.socket.util;



import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

/**
 * @author LongJianYong
 * @date 2014-3-3 ����03:52:16   
 * @version V1.0 
 * @description socketͨ��ʱʹ�õ����ݷ�װ��ת��
 */
public class CommunicationInfoUtil {

	private static Logger log = Logger.getLogger(CommunicationInfoUtil.class);
	/**
	 * @param infoLen
	 *            ��Ϣ�峤��
	 * @param byteBufferLen
	 *            ÿ��ByteBuffer����ĳ���
	 * @return ��ȡ�����õ�ByteBuffer����
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
	 * @param type  	�����������
	 * @param info		�����͵���Ϣ
	 * @return			���ط�װ�õķ�����Ϣ��
	 */
	public static ByteBuffer getSendInfoBody(int type,byte[] info){
		int len = 8+info.length;
//		����ת���������ֽ���
		byte[] typeByte = FormatTransferUtil.toHH(type);
		byte[] lenByte = FormatTransferUtil.toHH(len);
		log.debug("send data type:"+type+", len:"+len);
		byte[] sendInfo = new byte[len];
//		��Ϣ�����ɣ�len + type + info
		System.arraycopy(lenByte, 0, sendInfo, 0, lenByte.length);
		System.arraycopy(typeByte, 0, sendInfo, typeByte.length,
				typeByte.length);
		System.arraycopy(info, 0, sendInfo, lenByte.length + typeByte.length,
				info.length);
		log.info("the send ByteBuffer size="+sendInfo.length);
		return ByteBuffer.wrap(sendInfo);
	}
		
	/**
	 * @param byteBuffer	�������Ӧ���ص���Ϣͷ����
	 * @return				ת�������ֽ��򣬻�ȡ��Ӧ��Ϣ��ĳ��ȡ���Ӧ����
	 */
	public static int[] getRsqInfoHead(ByteBuffer byteBuffer){
		int[] head = new int[2];
		byte[] bt = new byte[4];
		byteBuffer.position(0);		//���û���ĵ�ǰλ��Ϊ0
		byteBuffer.get(bt);
//		�����ֽ���ת��������
		head[0] = FormatTransferUtil.hBytesToInt(bt);
		byteBuffer.get(bt);
		head[1] = FormatTransferUtil.hBytesToInt(bt);
		log.debug(byteBuffer.order().toString()+" receive data type:"+head[1]+", len:"+head[0]);
		return head;
	}
	
}
