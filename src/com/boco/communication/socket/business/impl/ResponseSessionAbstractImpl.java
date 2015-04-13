package com.boco.communication.socket.business.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.boco.communication.invoke.ResponseHandlerIF;
import com.boco.communication.socket.business.ResponseSessionAbstract;

/**
 * @author LongJianYong
 * @date 2014-2-26 下午04:34:12
 * @version V1.0
 * @description 设备接口业务处理实现类
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
	 * 业务处理
	 * 具体处理由各业务系统处理
	 */
	protected void rspHandler() {
		ResponseHandlerIF resIF = this.classMap.get(super.handlerClass);
		if(resIF == null){
		//	通过反射获取业务处理类
			resIF = getResponseHandlerIF(super.handlerClass);
		}
		resIF.responseHandler(super.responseType, super.responseData);
	}

	
	/**
	 * 获取业务处理类对象
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
