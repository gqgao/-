package com.ffcs.oss.fm.session.source;

import EPIRPSystem.*;
import ManagedGenericIRPConstDefs.StringTypeOpt;
import NotificationIRPSystem.NotificationIRP;
import NotificationIRPSystem.NotificationIRPHelper;
import net.sf.json.JSONObject;
import org.omg.CORBA.ORB;
import org.omg.CORBA.StringHolder;
import org.omg.CosEventComm.Disconnected;
import org.omg.CosNotification.EventType;
import org.omg.CosNotification.Property;
import org.omg.CosNotification.StructuredEvent;
import org.omg.CosNotifyComm.InvalidEventType;
import org.omg.CosNotifyComm.SequencePushConsumerPOA;
import org.omg.DynamicAny.DynAnyFactoryHelper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Service
public class Corba3GPPAlarmSourceSession extends AbstractAlarmSourceSession {
	private Logger LOGGER = LoggerFactory.getLogger(Corba3GPPAlarmSourceSession.class);
	private ORB orb;
	private EPIRP epirp;
	private NotificationIRP notificationIRP;
	private String subscriptionId;
	private String managerReference;

	@Autowired
	private CorbaProperties properties;

	@PostConstruct
	private void init() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				Corba3GPPAlarmSourceSession.this.destroy();
			}
		});
	}

	@Override
	public void ready() throws Exception {
		// 重连机制
		destroy();

		// 初始化orb
		initOrb();
		// 登陆
		login();
	}

	@Override
	public void destroy() {
		// 取消订阅
		unsubscribe();
		// 释放irp
		releaseIrp();
		orb = null;
		epirp = null;
		notificationIRP = null;
		managerReference = null;
		subscriptionId = null;
	}

	private void initOrb() throws Exception {
		try {
			if (orb == null) {
				LOGGER.info("初始化ORB");
				Properties props = new Properties();
				props.putAll(properties.getProps());
				orb = ORB.init(new String[0], props);
				LOGGER.info("orb初始化成功");
			}
		} catch (Exception e) {
			String expDesc = "ORB 初始化异常";
			LOGGER.error(expDesc, e);
			throw new Exception(expDesc);
		}
	}

	private void login() throws Exception {
		POA poa = POAHelper.narrow(orb
				.resolve_initial_references("RootPOA"));
		poa.the_POAManager().activate();
		DynAnyFactoryHelper.narrow(this.orb
				.resolve_initial_references("DynAnyFactory"));
		try {
			epirp = EPIRPHelper.narrow(orb.string_to_object(properties.getIor()));
		} catch (Exception e) {
			LOGGER.error("获取EP入口异常,确认IOR地址是否正确，ORB初始化是否正常 ：", e);
		}

		List<String> customIrps = new ArrayList<>();
		LOGGER.info("initNotification start!");
		notificationIRP = getNotificationIRP(epirp, "", customIrps);
		LOGGER.info("initNotification success!");

		managerReference = orb.object_to_string(new EventConsumer()._this(orb));

		StringTypeOpt stringtypeopt = new StringTypeOpt();
		stringtypeopt.value("");

		LOGGER.info("开始定购,[managerReference=" + managerReference + "]");
		subscriptionId = notificationIRP.attach_push(managerReference,
				0,
				customIrps.toArray(new String[customIrps.size()]),
				stringtypeopt);
		LOGGER.info("定购成功；[subscriptionId=" + subscriptionId + "]");
	}

	private NotificationIRP getNotificationIRP(EPIRP epirp, String iRPVersion, List<String> customIrps) throws Exception {
		try {
			SupportedIRPListTypeHolder irpList = new SupportedIRPListTypeHolder();
			ResultType resulttype = epirp.get_IRP_outline(iRPVersion, irpList);
			if (resulttype.value() != 0) {
				throw new Exception("不能获取outline信息 ");
			}

			String irpNotifiName = "";
			String irpNotifiId = "";
			//String irpNotifiClassName = "";
			for (SupportedIRPListTypeElement typeElement : irpList.value) {
				for (IRPElement element : typeElement.irpList) {
					for (String irpVersion : element.irpVersions) {
						if (!isEmpty(irpVersion)) {
							LOGGER.info("irpVersion : " + irpVersion + ",irpId:" + element.irpId);
							String irpUpperName = irpVersion.toUpperCase().replace(" ", "_");
							if (irpUpperName.startsWith("NOTIFICATION_IRP")) {
								irpNotifiName = irpUpperName;
								irpNotifiId = element.irpId;
								//irpNotifiClassName = element.irpClassName;
							}
							String customIrpTypes = properties.getIrpTypes();
							if (!isEmpty(customIrpTypes)) {
								for (String customIrpType : customIrpTypes.split(",")) {
									if (irpUpperName.startsWith(customIrpType.trim())) {
										customIrps.add(irpVersion);
									}
								}
							}
						}
					}
				}
			}

			String systemDN = irpList.value[0].systemDN;

			StringHolder stringholder = new StringHolder();
			LOGGER.info("getNotification systemDN:" + systemDN
					+ ";irpId : " + irpNotifiId
					+ ";IrpNotifName : " + irpNotifiName);
			epirp.get_IRP_reference(irpNotifiName, systemDN, irpNotifiId, stringholder);
			LOGGER.info("irpreference:" + stringholder.value);

			return NotificationIRPHelper.narrow(orb.string_to_object(stringholder.value));
		} catch (Exception e) {
			String expDesc = "获取NotificationIRP出错";
			LOGGER.error(expDesc, e);
			throw new Exception(expDesc);
		}
	}

	private void releaseIrp() {
		try {
			notificationIRP._release();
		} catch (Exception ex) {
			this.LOGGER.error("释放notificationIRP出错，该异常可不关注！");
		}
		try {
			epirp._release();
			LOGGER.info("release epirp success!!");
		} catch (Exception e) {
			LOGGER.error("释放epirp出错，该异常可不关注！");
		}
		try {
			orb.shutdown(true);
			LOGGER.info("shutdown orb success!!");
		} catch (Exception e) {
			LOGGER.error("释放orb出错，该异常可不关注！");
		}
	}

	private void unsubscribe() {
		try {
			if (notificationIRP != null) {
				LOGGER.info("撤消定购开始；定购号:" + subscriptionId + ";事件接收者对象的引用:"
						+ managerReference);
				notificationIRP.detach(managerReference, subscriptionId);
				LOGGER.info("撤消定购成功；");
			}
		} catch (Exception e) {
			LOGGER.error("撤销订购失败：", e);
		}
	}

	private boolean isEmpty(String string) {
		return string == null | "".equals(string);
	}

	/**
	 * Corba事件通知消费者
	 */
	private class EventConsumer extends SequencePushConsumerPOA {
		private Logger LOGGER = LoggerFactory.getLogger(EventConsumer.class);

		// push事件
		public void push_structured_events(StructuredEvent astructuredevent[])
				throws Disconnected {
			for (StructuredEvent structuredEvent : astructuredevent) {
				try {
					String domain_name = structuredEvent.header.fixed_header.event_type.domain_name;
					String type_name = structuredEvent.header.fixed_header.event_type.type_name;

					LOGGER.info("domain_name:" + domain_name + ";type_name:" + type_name);

					JSONObject jsonObject = new JSONObject();
					jsonObject.put("domain_name", domain_name);
					jsonObject.put("type_name", type_name);
					jsonObject.put("event_name", structuredEvent.header.fixed_header.event_name);
					for (Property prop : structuredEvent.filterable_data) {
						jsonObject.put(prop.name, CorbaUtils.extractAny(prop.value));
					}
					jsonObject.put("remainder_of_body", CorbaUtils.extractAny(structuredEvent.remainder_of_body));

					addAlarm(jsonObject.toString());
				} catch (Exception e) {
					LOGGER.error("事件处理出错：", e);
				}
			}
		}

		public void disconnect_sequence_push_consumer() {
			LOGGER.info("disconnect_sequence_push_consumer . . .");
		}

		public void offer_change(EventType aeventtype[], EventType aeventtype1[])
				throws InvalidEventType {
			LOGGER.info("offer_change . . .");
		}
	}
}