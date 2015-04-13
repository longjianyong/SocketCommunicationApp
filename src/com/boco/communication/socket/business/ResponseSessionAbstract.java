package com.boco.communication.socket.business;


 /**
 * @author LongJianYong
 * @date 2014-2-28 下午02:57:36   
 * @version V1.0 
 * @description TODO
 */
public abstract class ResponseSessionAbstract {
	
	protected String handlerClass = null;
	protected byte[] responseData = null;
	protected int responseType = -1;

	public synchronized boolean handlerResponse(String handlerClass, int type , byte[] rsp){
		this.handlerClass = handlerClass;
		this.responseData = rsp;
		this.responseType = type;
//		唤醒等待
		this.notify();
		return true;
	}

	/**
	 * 		等待响应的处理线程
	 */
	public synchronized void waitRspHandler(){
		while(true){
			while(handlerClass == null && responseData == null && responseType == -1){
				try {
					this.wait();
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			rspHandler();
			destoryResData();
		}
	}
	
	private void destoryResData(){
		handlerClass = null;
		this.responseData = null;
		this.responseType = -1;
	}
	
	protected abstract void rspHandler(); 
}
