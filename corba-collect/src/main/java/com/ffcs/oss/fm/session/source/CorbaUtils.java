package com.ffcs.oss.fm.session.source;

import AlarmIRPConstDefs.*;
import ManagedGenericIRPConstDefs.IRPTimeHelper;
import net.sf.json.JSONObject;
import org.omg.CORBA.Any;
import org.omg.CORBA.ArrayDefHelper;
import org.omg.CORBA.TCKind;
import org.omg.TimeBase.UtcT;
import org.omg.TimeBase.UtcTHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;

/**
 * Created by ${gujj} on 2017-08-23.
 */
public class CorbaUtils {
	// 文件流
	private static Logger LOGGER = LoggerFactory.getLogger(CorbaUtils.class);

	private static final String CORBA_ENCODING = "ISO-8859-1";

	private static String extract_string(Any any) {
		String value = "";
		int kind = any.type().kind().value();
		if (kind == TCKind._tk_string) {
			value = any.extract_string();
		} else if (kind == TCKind._tk_wstring) {
			value = any.extract_wstring();
		} else if (kind == TCKind._tk_long) {
			value = String.valueOf(any.extract_long());
		} else if (kind == TCKind._tk_longlong) {
			value = String.valueOf(any.extract_longlong());
		} else if (kind == TCKind._tk_double) {
			value = String.valueOf(any.extract_double());
		} else if (kind == TCKind._tk_float) {
			value = String.valueOf(any.extract_float());
		} else if (kind == TCKind._tk_char) {
			value = String.valueOf(any.extract_char());
		} else if (kind == TCKind._tk_short) {
			value = String.valueOf(any.extract_short());
		} else if (kind == TCKind._tk_boolean) {
			value = String.valueOf(any.extract_boolean());
		} else {
			value = any.extract_string();
		}
		return value;
	}

	public static String extractAny(Any any) {
		String result = "";
		try {
			int kind = any.type().kind().value();
			switch (kind) {
			case TCKind._tk_any:
				result += extractAny(any.extract_any());
				break;
			case TCKind._tk_array:
				result += ArrayDefHelper.extract(any);
				break;
			case TCKind._tk_boolean:
				result += any.extract_boolean();
				break;
			case TCKind._tk_char:
				result += any.extract_char();
				break;
			case TCKind._tk_double:
				result += any.extract_double();
				break;
			case TCKind._tk_fixed:
				result += any.extract_fixed();
				break;
			case TCKind._tk_float:
				result += any.extract_float();
				break;
			case TCKind._tk_long:
				result += any.extract_long();
				break;
			case TCKind._tk_longdouble:
				result += any.extract_double();
				break;
			case TCKind._tk_longlong:
				result += any.extract_longlong();
				break;
			case TCKind._tk_short:
				result += any.extract_short();
				break;
			case TCKind._tk_struct:
				if (any.type().name().equals("UtcT")) {
					result += getUtcTime(UtcTHelper.extract(any));
				} else if (any.type().name().equals("ThresholdInfoType")) {
					LOGGER.info("***_tk_struct TypeName***" + any.type().name());
					ThresholdInfoType thresholdInfo = ThresholdInfoTypeHelper.extract(any);
					result += thresholdInfo.armTime + "," + thresholdInfo.attributeID + ","
							+ thresholdInfo.observedValue + "," + thresholdInfo.thresholdLevel.value();
				} else {
					result += "***_tk_struct UnknownTypeName***" + any.type().name();
				}
				break;
			case TCKind._tk_string:
				result += any.extract_string().trim();
				break;
			case TCKind._tk_octet:
				result += any.extract_octet();
				break;
			case TCKind._tk_ulong:
				result += any.extract_ulong();
				break;
			case TCKind._tk_ulonglong:
				result += any.extract_ulonglong();
				break;
			case TCKind._tk_ushort:
				result += any.extract_ushort();
				break;
			case TCKind._tk_wchar:
				result = "" + any.extract_wchar();
				break;
			case TCKind._tk_wstring:
				result = any.extract_wstring().trim();
				break;
			case TCKind._tk_alias:
				JSONObject jsonObject = new JSONObject();
				if (any.type().name().equals("IRPTime")) {
					result = getUtcTime(IRPTimeHelper.extract(any));
				} else if (any.type().name().equals("AttributeSetType")) {
					LOGGER.info("***_tk_alias TypeName***" + any.type().name());
					for (AttributeValueType attributeValueType : AttributeSetTypeHelper.extract(any)) {
						jsonObject.put(attributeValueType.attribute_name, extract_string(attributeValueType.value));
					}
					result = jsonObject.toString();
				} else if (any.type().name().equals("AttributeChangeSetType")) {
					LOGGER.info("***_tk_alias TypeName***" + any.type().name());
					for (AttributeValueChangeType attributeValueType : AttributeChangeSetTypeHelper.extract(any)) {
						jsonObject.put(attributeValueType.new_value, extract_string(attributeValueType.new_value));
					}
					result = jsonObject.toString();
				} else {
					result = "***_tk_alias UnknownTypeName***" + any.type().name();
				}
				break;
			case TCKind._tk_enum:
				if (any.type().name().equals("TrendIndication")) {
					result = Integer.toString(TrendIndicationTypeHelper.extract(any).value());
				} else {
					result = "***_tk_enum UnknownTypeName***" + any.type().name();
				}
				break;
			default:
				try {
					result = "[ParseUnknownTCKind " + kind + ",kindName=" + any.type().name() + "]";
				} catch (Exception e) {
					result = "[ParseUnknownTCKind=" + kind + "]";
				}
			}
			return convertString(result, "GBK");
		} catch (Exception e) {
			LOGGER.error("提取Any类型值出错：" + any, e);
			return "[ERROR] " + e.getMessage();
		}
	}

	private static String getUtcTime(UtcT utct) {
		long beginTime = 122192928000000000L;
		long utcTime = (utct.time - beginTime) / 10000L;
		return String.valueOf(new Timestamp(utcTime));
	}

	/**
	 * 转换字符串编码
	 * 
	 * @param content
	 *            字符串内容
	 * @param charEncoding
	 *            字符编码
	 * @return
	 */
	private static String convertString(String content, String charEncoding) {
		try {
			return new String(content.getBytes(CORBA_ENCODING), charEncoding);
		} catch (UnsupportedEncodingException e) {
			LOGGER.warn("字符编码转换失败", e);
			return content;
		}
	}
}
