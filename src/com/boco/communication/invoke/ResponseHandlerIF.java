package com.boco.communication.invoke;

/**
 * @author Longjianyong
 *	ҵ����ӿ�
 */
public interface ResponseHandlerIF {

	/**
	 * @param type
	 * @param date
	 * @return	ҵ����������
	 */
	public boolean responseHandler(int type, byte[] date);
	
}
