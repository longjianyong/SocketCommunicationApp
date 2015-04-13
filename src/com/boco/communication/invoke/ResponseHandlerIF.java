package com.boco.communication.invoke;

/**
 * @author Longjianyong
 *	业务处理接口
 */
public interface ResponseHandlerIF {

	/**
	 * @param type
	 * @param date
	 * @return	业务处理函数，由
	 */
	public boolean responseHandler(int type, byte[] date);
	
}
