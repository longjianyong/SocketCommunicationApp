package com.boco.communication.socket.util;

/**
 * @author LongJianYong
 * @date 2014-2-28 ����03:04:41   
 * @version V1.0 
 * @description ���Ƽ���selector���¼�����
 */
public class RegisteEvent {
//	type��ȡֵ����Ҫ��ʲô���͵Ĳ���
	public final static int SELECTOR_REGISTER_CHANNEL = 1;
	public final static int SELECTOR_CHANGE_OPS = 2;
	public final static int SELECTOR_EVENT = 3;
//	�Զ������ͣ����Ͷ����¼�/���ӳ�ʱ�¼�
	public final static int EVE_SEND_EVENT = 1 << 5;
	public final static int EVE_TIMEOUT_EVENT = 1 << 6;
//	���¼�
	public final static int EVE_NONE = -1;
	
//	���Ӷ���
	public Object eventObj;
//	ע���������
	public int type;
//	ע��ļ�ֵ
	public int ops;
	
	public RegisteEvent(Object eventObj , int type , int ops){
		this.eventObj = eventObj;
		this.type = type;
		this.ops = ops;
	}
}
