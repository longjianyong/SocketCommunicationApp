package com.boco.communication.socket.util;


import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

/**
 * ͨ�Ÿ�ʽת��
 * @author LongJianYong
 * @date 2014-3-3 ����03:58:07   
 * @version V1.0 
 * @description TODO
 * Java ��һЩwindows���������c��c++��delphi��д������������ͨѶʱ����Ҫ������Ӧ��ת�� �ߡ����ֽ�֮���ת��
 * windows���ֽ���Ϊ���ֽڿ�ͷ. linux,unix���ֽ���Ϊ���ֽڿ�ͷ java������ƽ̨�仯�����Ǹ��ֽڿ�ͷ
 */
public class FormatTransferUtil {
	
	public static String strEncode = "UTF-8";
	
	/**
	 * �� intתΪ���ֽ���ǰ�����ֽ��ں��byte����
	 * ת�������ֽ���
	 * @param n
	 *            int
	 * @return byte[]
	 */
	public static byte[] toLH(int n) {
		byte[] b = new byte[4];
		b[0] = (byte) (n & 0xff);
		b[1] = (byte) (n >> 8 & 0xff);
		b[2] = (byte) (n >> 16 & 0xff);
		b[3] = (byte) (n >> 24 & 0xff);
		return b;
	}

	/**
	 * �� intתΪ���ֽ���ǰ�����ֽ��ں��byte����
	 * ת�������ֽ���
	 * @param n
	 * int
	 * @return byte[]
	 */
	public static byte[] toHH(int n) {
		byte[] b = new byte[4];
		b[3] = (byte) (n & 0xff);
		b[2] = (byte) (n >> 8 & 0xff);
		b[1] = (byte) (n >> 16 & 0xff);
		b[0] = (byte) (n >> 24 & 0xff);
		return b;
	}
	/**
	 * ����intתΪ���ֽ���ǰ�����ֽ��ں��byte����
	 *  public static byte[] toHH(int number) { int
	 * temp = number; byte[] b = new byte[4]; for (int i = b.length - 1; i > -1;
	 * i--) { b = new Integer(temp & 0xff).byteValue(); temp = temp >> 8; }
	 * return b; } public static byte[] IntToByteArray(int i) { byte[] abyte0 =
	 * new byte[4]; abyte0[3] = (byte) (0xff & i); abyte0[2] = (byte) ((0xff00 &
	 * i) >> 8); abyte0[1] = (byte) ((0xff0000 & i) >> 16); abyte0[0] = (byte)
	 * ((0xff000000 & i) >> 24); return abyte0; }
	 */

	/**
	 * �� shortתΪ���ֽ���ǰ�����ֽ��ں��byte����
	 * 
	 * @param n
	 *            short
	 * @return byte[]
	 */
	public static byte[] toLH(short n) {
		byte[] b = new byte[2];
		b[0] = (byte) (n & 0xff);
		b[1] = (byte) (n >> 8 & 0xff);
		return b;
	}

	/**
	 * �� shortתΪ���ֽ���ǰ�����ֽ��ں��byte����
	 * 
	 * @param n
	 *            short
	 * @return byte[]
	 */
	public static byte[] toHH(short n) {
		byte[] b = new byte[2];
		b[1] = (byte) (n & 0xff);
		b[0] = (byte) (n >> 8 & 0xff);
		return b;
	}


	/**
	 * �� floatתΪ���ֽ���ǰ�����ֽ��ں��byte����
	 */
	public static byte[] toLH(float f) {
		return toLH(Float.floatToRawIntBits(f));
	}

	/**
	 * �� floatתΪ���ֽ���ǰ�����ֽ��ں��byte����
	 */
	public static byte[] toHH(float f) {
		return toHH(Float.floatToRawIntBits(f));
	}

	/**
	 * �� StringתΪbyte����
	 */
	public static byte[] stringToBytes(String s, int length) {
		while (s.getBytes().length < length) {
			s += " ";
		}
		return s.getBytes();
	}

	/**
	 * ���ֽ�����ת��ΪString
	 * 
	 * @param b
	 *            byte[]
	 * @return String
	 */
	public static String bytesToString(byte[] b) {
		StringBuffer result = new StringBuffer("");
		int length = b.length;
		for (int i = 0; i < length; i++) {
			result.append((char) (b[i] & 0xff));
		}
		return result.toString();
	}

	/**
	 * ���ַ���ת��Ϊbyte����
	 * 
	 * @param s
	 *            String
	 * @return byte[]
	 */
	public static byte[] stringToBytes(String s) {
		return s.getBytes();
	}

	/**
	 * �����ֽ�����ת��Ϊint
	 * 
	 * @param b
	 *            byte[]
	 * @return int
	 */
	public static int hBytesToInt(byte[] b) {
		int s = 0;
		for (int i = 0; i < 3; i++) {
			if (b[i] >= 0) {
				s = s + b[i];
			} else {
				s = s + 256 + b[i];
			}
			s = s * 256;
		}
		if (b[3] >= 0) {
			s = s + b[3];
		} else {
			s = s + 256 + b[3];
		}
		return s;
	}

	/**
	 * �����ֽ�����ת��Ϊint
	 * 
	 * @param b
	 *            byte[]
	 * @return int
	 */
	public static int lBytesToInt(byte[] b) {
		int s = 0;
		for (int i = 0; i < 3; i++) {
			if (b[3 - i] >= 0) {
				s = s + b[3 - i];
			} else {
				s = s + 256 + b[3 - i];
			}
			s = s * 256;
		}
		if (b[0] >= 0) {
			s = s + b[0];
		} else {
			s = s + 256 + b[0];
		}
		return s;
	}

	/**
	 * ���ֽ����鵽short��ת��
	 * 
	 * @param b
	 *            byte[]
	 * @return short
	 */
	public static short hBytesToShort(byte[] b) {
		int s = 0;
		if (b[0] >= 0) {
			s = s + b[0];
		} else {
			s = s + 256 + b[0];
		}
		s = s * 256;
		if (b[1] >= 0) {
			s = s + b[1];
		} else {
			s = s + 256 + b[1];
		}
		short result = (short) s;
		return result;
	}

	/**
	 * ���ֽ����鵽short��ת��
	 * 
	 * @param b
	 *            byte[]
	 * @return short
	 */
	public static short lBytesToShort(byte[] b) {
		int s = 0;
		if (b[1] >= 0) {
			s = s + b[1];
		} else {
			s = s + 256 + b[1];
		}
		s = s * 256;
		if (b[0] >= 0) {
			s = s + b[0];
		} else {
			s = s + 256 + b[0];
		}
		short result = (short) s;
		return result;
	}

	/**
	 * ���ֽ�����ת��Ϊfloat
	 * 
	 * @param b
	 *            byte[]
	 * @return float
	 */
	public static float hBytesToFloat(byte[] b) {
		int i = 0;
		Float F = new Float(0.0);
		i = ((((b[0] & 0xff) << 8 | (b[1] & 0xff)) << 8) | (b[2] & 0xff)) << 8
				| (b[3] & 0xff);
		return F.intBitsToFloat(i);
	}

	/**
	 * ���ֽ�����ת��Ϊfloat
	 * 
	 * @param b
	 *            byte[]
	 * @return float
	 */
	public static float lBytesToFloat(byte[] b) {
		int i = 0;
		Float F = new Float(0.0);
		i = ((((b[3] & 0xff) << 8 | (b[2] & 0xff)) << 8) | (b[1] & 0xff)) << 8
				| (b[0] & 0xff);
		return F.intBitsToFloat(i);
	}

	/**
	 * �� byte�����е�Ԫ�ص�������
	 */
	public static byte[] bytesReverseOrder(byte[] b) {
		int length = b.length;
		byte[] result = new byte[length];
		for (int i = 0; i < length; i++) {
			result[length - i - 1] = b[i];
		}
		return result;
	}

	/**
	 * ��ӡbyte����
	 */
	public static void printBytes(byte[] bb) {
		int length = bb.length;
		for (int i = 0; i < length; i++) {
			System.out.print(bb + " ");
		}
		System.out.println("");
	}

	public static void logBytes(byte[] bb) {
		int length = bb.length;
		String out = "";
		for (int i = 0; i < length; i++) {
			out = out + bb + " ";
		}

	}

	/**
	 * �� int���͵�ֵת��Ϊ�ֽ���ߵ�������Ӧ��intֵ
	 * 
	 * @param i
	 *            int
	 * @return int
	 */
	public static int reverseInt(int i) {
		int result = FormatTransferUtil.hBytesToInt(FormatTransferUtil.toLH(i));
		return result;
	}

	/**
	 * �� short���͵�ֵת��Ϊ�ֽ���ߵ�������Ӧ��shortֵ
	 * 
	 * @param s
	 *            short
	 * @return short
	 */
	public static short reverseShort(short s) {
		short result = FormatTransferUtil.hBytesToShort(FormatTransferUtil.toLH(s));
		return result;
	}

	/**
	 * �� float���͵�ֵת��Ϊ�ֽ���ߵ�������Ӧ��floatֵ
	 * 
	 * @param f
	 *            float
	 * @return float
	 */
	public static float reverseFloat(float f) {
		float result = FormatTransferUtil.hBytesToFloat(FormatTransferUtil.toLH(f));
		return result;
	}

	/**
	 * ��byte[]ת����int
	 * 
	 * @param f
	 *            float
	 * @return float
	 */
	public static int toInt(byte[] bRefArr) {
	    int iOutcome = 0;
	    byte bLoop;
	    for (int i = 0; i < bRefArr.length; i++) {
	        bLoop = bRefArr[i];
	        iOutcome += (bLoop & 0xFF) << (8 * i);
	    }
	    return iOutcome;
	}
	
	/**
	 * ��intת����byte[]
	 * 
	 * @param f
	 *            float
	 * @return float
	 */
	public static byte[] toByteArray(int iSource, int iArrayLen) {
	    byte[] bLocalArr = new byte[iArrayLen];
	    for (int i = 0; (i < 4) && (i < iArrayLen); i++) {
	        bLocalArr[i] = (byte) (iSource >> 8 * i & 0xFF);
	    }
	    return bLocalArr;
	}
	
	/**
	 * @param str
	 * @return String�ַ���ת����buffer����
	 */
	public static ByteBuffer getByteBuffer(String str) {
		return ByteBuffer.wrap(str.getBytes());
	}

	/**
	 * @param buffer
	 *            ��Ҫת����buffer����
	 * @return bufferת����String���͵��ַ���
	 */
	public static String getString(ByteBuffer buffer) {
		Charset charset = null;
		CharsetDecoder decoder = null;
		CharBuffer charBuffer = null;
		try {
			charset = Charset.forName(strEncode);
			decoder = charset.newDecoder();
			// charBuffer = decoder.decode(buffer);//������Ļ���ֻ�������һ�ν�����ڶ�����ʾΪ��
			charBuffer = decoder.decode(buffer.asReadOnlyBuffer());
			return charBuffer.toString();
		} catch (Exception ex) {
			ex.printStackTrace();
			return "";
		}
	}

	
	
}