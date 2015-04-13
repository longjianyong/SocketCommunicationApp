package com.boco.communication.socket.util;

/**
 * @author LongJianYong
 * @date 2014-2-28 下午03:04:41   
 * @version V1.0 
 * @description 控制监听selector的事件处理
 */
public class RegisteEvent {
//	type的取值，需要做什么类型的操作
	public final static int SELECTOR_REGISTER_CHANNEL = 1;
	public final static int SELECTOR_CHANGE_OPS = 2;
	public final static int SELECTOR_EVENT = 3;
//	自定义类型，发送队列事件/连接超时事件
	public final static int EVE_SEND_EVENT = 1 << 5;
	public final static int EVE_TIMEOUT_EVENT = 1 << 6;
//	空事件
	public final static int EVE_NONE = -1;
	
//	链接对象
	public Object eventObj;
//	注册操作类型
	public int type;
//	注册的键值
	public int ops;
	
	public RegisteEvent(Object eventObj , int type , int ops){
		this.eventObj = eventObj;
		this.type = type;
		this.ops = ops;
	}
}
