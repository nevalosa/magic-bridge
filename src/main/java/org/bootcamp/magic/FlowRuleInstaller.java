package org.bootcamp.magic;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowRuleInstaller implements DeviceListener {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private static final int DROP_PRIORITY = 0;
	private static final int HIGH_PRIORITY = 65535;
	private static final int DEFAULT_PRIORITY = 65535;
	private static final int TABLE_0 = 0;
	private static final int TABLE_1 = 0;
	private FlowRuleService flowRuleService;
	private DeviceService deviceService;
	
	private ApplicationId appId;
	private DeviceId deviceId;

	public FlowRuleInstaller(DeviceService deviceService, FlowRuleService flowRuleService) {
		this.flowRuleService = flowRuleService;
		this.deviceService = deviceService;
	}

	@Override
	public void event(DeviceEvent event) {
		DeviceEvent.Type type = event.type();
		switch (type) {
		case DEVICE_ADDED:
			processAddEvent(event);
			break;
		default:
			log.info("Device Event:type={}, port={}",event.type(), event.port().number().toLong());
			return;
		}
	}

	private void processAddEvent(DeviceEvent event) {
		log.info("Device ADD Event:type={}, port={}",event.type(), event.port().number().toLong());
	}

	public void defaultFlowRule(boolean install) {
		TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
		TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();

		treatment.wipeDeferred();
		treatment.drop();

		FlowRule flowRule = DefaultFlowRule.builder().forDevice(deviceId).withSelector(selector.build())
				.withTreatment(treatment.build()).withPriority(HIGH_PRIORITY).fromApp(appId).makePermanent()
				.forTable(TABLE_0).build();

		applyRules(install, flowRule);
	}

	private void applyRules(boolean install, FlowRule flowRule) {

		FlowRuleOperations.Builder flowOpsBuilder = FlowRuleOperations.builder();

		flowOpsBuilder = install ? flowOpsBuilder.add(flowRule) : flowOpsBuilder.remove(flowRule);

		flowRuleService.apply(flowOpsBuilder.build(new FlowRuleOperationsContext() {
			@Override
			public void onSuccess(FlowRuleOperations ops) {
				log.debug("Provisioned vni or forwarding table");
			}

			@Override
			public void onError(FlowRuleOperations ops) {
				log.debug("Failed to privision vni or forwarding table");
			}
		}));
	}

}
