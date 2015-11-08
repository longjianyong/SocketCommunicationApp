package com.boco.communication.test;

public class Test {

	public static void me1(String asd){
		asd = null;
	}
	
	public static void main(String[] args) {
		String asd = new String("456");
		me1(asd);
		System.out.println(asd);
		System.out.println(me2());
	}
	
	public static String me2(){
		String s = "ooo";
		try{
			return s;
		}
		finally{
			s = "ppp";
			System.out.println("99999"+s);
		}
	}
}
