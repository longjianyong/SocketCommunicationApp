package com.boco.communication.test;

import com.boco.communication.invoke.util.RequestHandlerUtil;
import com.boco.communication.socket.bean.AdamEnum;
import com.boco.communication.socket.bean.SessionInfo;

public class Client {

	public static void main(String[] args) {
//		git分布式项目代码管理
		SessionInfo sessionI= new SessionInfo("127.0.0.1", 8999);
		sessionI.setSessionType(2);
		RequestHandlerUtil.send(sessionI, AdamEnum.ADAM_HEAD_TYPE_ENUM.WEB_2_PROBER_REQUEST_VALUE, "ddd".getBytes());
	}
}
