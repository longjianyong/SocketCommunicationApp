package com.boco.communication.socket.business.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.boco.communication.invoke.ResponseHandlerIF;
import com.boco.communication.socket.business.ResponseSessionAbstract;

/**
 * @author LongJianYong
 * @date 2014-2-26 ����04:34:12
 * @version V1.0
 * @description �豸�ӿ�ҵ����ʵ����
 */
public class ResponseSessionAbstractImpl extends ResponseSessionAbstract implements Runnable {

	private Logger logger = Logger.getLogger(ResponseSessionAbstractImpl.class);

	private static ResponseSessionAbstractImpl handler = null;

	private Map<String, ResponseHandlerIF> classMap = new HashMap<String, ResponseHandlerIF>();
	
	private ResponseSessionAbstractImpl() {

	}

	public static synchronized ResponseSessionAbstract getResponseHandlerImpl() {
		if (handler == null) {
			handler = new ResponseSessionAbstractImpl();
			new Thread(handler).start();
		}
		return handler;
	}

	/* 
	 * ҵ����
	 * ���崦���ɸ�ҵ��ϵͳ����
	 */
	protected void rspHandler() {
		ResponseHandlerIF resIF = this.classMap.get(super.handlerClass);
		if(resIF == null){
		//	ͨ�������ȡҵ������
			resIF = getResponseHandlerIF(super.handlerClass);
		}
		resIF.responseHandler(super.responseType, super.responseData);
	}

	
	/**
	 * ��ȡҵ���������
	 * @param clazz
	 * @return
	 */
	private ResponseHandlerIF getResponseHandlerIF(String clazz) {
		
		ResponseHandlerIF  resIF = null;
		try {
			Class<?> ownerClass = Class.forName(clazz);
			resIF = (ResponseHandlerIF) ownerClass.newInstance();
			this.classMap.put(clazz, resIF);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			logger.error("Can not instance of the '" + clazz
					+ "': Class not found! " + e.getLocalizedMessage());
		} catch (InstantiationException e) {
			e.printStackTrace();
			logger.error("Can not instance of the '" + clazz
					+ "': Instantiation fails! " + e.getLocalizedMessage());
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			logger.error("Can not instance of the '" + clazz
					+ "': Illegal Access! " + e.getLocalizedMessage());
		}
		return resIF;
	}
	
	@Override
	public void run() {
		super.waitRspHandler();
	}
}
