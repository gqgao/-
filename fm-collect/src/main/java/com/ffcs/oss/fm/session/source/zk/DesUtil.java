package com.ffcs.oss.fm.session.source.zk;

import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DES算法的入口参数有三个�? KEY、Data、Mode�? 其中Key�?8个字节共64位，是DES算法的工作密钥；
 * Data也为8个字�?64位，是要被加密或被解密的数据�? Mode为DES的工作方式，有两种：加密或解密�??
 * 
 * @author lnf
 * 
 */
public class DesUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(DesUtil.class);

	private Cipher encoder;

	private Cipher decoder;

	/**
	 * 初始�?
	 * 
	 * @param keySpec
	 *            密钥
	 * @param cbcMode
	 *            CBC模式�?
	 * @param rsa
	 *            填充方式
	 * @param operation
	 *            初始向量
	 */
	public DesUtil(String keySpec, String cbcMode, String rsa, String operation) {
		// 加密模式为DES模式，密钥为1234abcd，模式为：CBC，填充模式为：Zeros，初始向量为�?00000000，编码格式为：UTF8�?
		try {
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
			DESKeySpec desKeySpec = new DESKeySpec(keySpec.getBytes("ASCII"));// 这里的ASCII是为了保证只有ASCII字符
			SecretKey secretKey = keyFactory.generateSecret(desKeySpec);
			IvParameterSpec iv = new IvParameterSpec(operation.getBytes("ASCII"));// 前提已经满足，所以可以换做其它编�?

			encoder = Cipher.getInstance("DES/" + cbcMode + "/" + rsa);
			decoder = Cipher.getInstance("DES/" + cbcMode + "/" + rsa);
			encoder.init(Cipher.ENCRYPT_MODE, secretKey, iv);
			decoder.init(Cipher.DECRYPT_MODE, secretKey, iv);
		} catch (Exception e) {
			LOGGER.info("初始化加解密错误", e);
		}
	}

	// 加密
	public String encrypt(String plainText) throws Exception {
		byte[] inputs = plainText.getBytes("UTF-8");
		int length = inputs.length, mod = length % 8;
		if (mod > 0) {// 如果不是8的�?�数，则扩充�?8的�?�数
			length += 8 - mod;
		}
		inputs = Arrays.copyOf(inputs, length);// 空出的位用零填充
		byte[] encoded = encoder.doFinal(inputs);
		StringBuilder buf = new StringBuilder();
		for (byte b : encoded) {
			buf.append(String.format("%02X", b));// 16进制大写输出，不足两位前补零
		}
		return buf.toString();
	}

	// 解密
	public String decrypt(String encodedText) throws Exception {
		byte[] inputs = new byte[encodedText.length() / 2];
		for (int i = 0; i < inputs.length; i++) {
			inputs[i] = (byte) Integer.parseInt(encodedText.substring(i * 2, i * 2 + 2), 16);
		}
		byte[] decoded = decoder.doFinal(inputs);
		return new String(decoded, "UTF-8").trim();// 记得清除加密时补充的零�??
	}
}
