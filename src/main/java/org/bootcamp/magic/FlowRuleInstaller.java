package org.bootcamp.magic;

import java.net.URI;
import java.util.Set;
import java.util.TreeSet;

import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.Device;
import org.onosproject.net.Device.Type;
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
	private static final int HIGH_PRIORITY = 65535;
	private static final int DEFAULT_PRIORITY = 60000;
	private static final int TABLE_0 = 0;
	private static final int TABLE_1 = 0;
	private static final IpPrefix PREFEX_24 = Ip4Prefix.valueOf("10.0.0.0/24");
	private FlowRuleService flowRuleService;
	private DeviceService deviceService;
	private ApplicationId appId;
	private Set<URI> set = new TreeSet<URI>();
	
	public FlowRuleInstaller(
			ApplicationId appId,
			DeviceService deviceService, 
			FlowRuleService flowRuleService) {
		
		this.appId = appId;
		this.flowRuleService = flowRuleService;
		this.deviceService = deviceService;
	}

	@Override
	public void event(DeviceEvent event) {
		DeviceEvent.Type type = event.type();
		switch (type) {
		case DEVICE_ADDED:
			log.info("Device ADD Event.");
			processAddEvent(event);
			break;
		default:
			log.info("Device Event:{}",event.toString());
			return;
		}
	}

	private void processAddEvent(DeviceEvent event) {
		log.info("ADD==Device Event:{}",event.toString());
		Iterable<Device> devices = deviceService.getAvailableDevices(Type.SWITCH);
		
		for(Device device : devices){
			log.info("Iterate added device.");
			DeviceId deviceId = device.id();
			
			if(set.contains(deviceId.uri())){
				log.info("Device already added. Flow exist.");
			}else{
				log.info("Add default flow to device.");
				cleanTable0(deviceId);
				defaultFlowRule(deviceId);
				addFlowTable0(deviceId);
				addFlowTable1(deviceId);
				set.add(deviceId.uri());
			}
		}
	}
	
	public void cleanTable0(DeviceId deviceId){
		log.info("Clean Table 0.");
		FlowRule flowRule = DefaultFlowRule.builder().forDevice(deviceId).fromApp(appId).makePermanent()
				.forTable(TABLE_0).build();
		applyRules(false, flowRule);
	}

	public void defaultFlowRule(DeviceId deviceId) {
		
		TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
		TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
		treatment.drop();

		FlowRule flowRule = DefaultFlowRule.builder().forDevice(deviceId).withSelector(selector.build())
				.withTreatment(treatment.build()).withPriority(HIGH_PRIORITY).fromApp(appId).makePermanent()
				.forTable(TABLE_0).build();
		applyRules(true, flowRule);
	}
	
	public void addFlowTable0(DeviceId deviceId) {
		
		TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
		selector.matchIPSrc(PREFEX_24).matchEthType(Ethernet.TYPE_IPV4);
		
		TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
		treatment.transition(TABLE_1);
		
		FlowRule flowRule = DefaultFlowRule.builder().forDevice(deviceId).withSelector(selector.build())
				.withTreatment(treatment.build()).withPriority(DEFAULT_PRIORITY).fromApp(appId).makePermanent()
				.forTable(TABLE_0).build();
		applyRules(true, flowRule);
	}
	
	public void addFlowTable1(DeviceId deviceId) {
		
		TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
		selector.matchEthDst(MacAddress.NONE);
		
		TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
		treatment.drop();
		
		FlowRule flowRule = DefaultFlowRule.builder().forDevice(deviceId).withSelector(selector.build())
				.withTreatment(treatment.build()).withPriority(DEFAULT_PRIORITY).fromApp(appId).makePermanent()
				.forTable(TABLE_1).build();
		applyRules(true, flowRule);
	}

	private void applyRules(boolean install, FlowRule flowRule) {

		FlowRuleOperations.Builder flowOpsBuilder = FlowRuleOperations.builder();

		flowOpsBuilder = install ? flowOpsBuilder.add(flowRule) : flowOpsBuilder.remove(flowRule);

		flowRuleService.apply(flowOpsBuilder.build(new FlowRuleOperationsContext() {
			@Override
			public void onSuccess(FlowRuleOperations ops) {
				log.info("Flow add onSuccess.");
			}

			@Override
			public void onError(FlowRuleOperations ops) {
				log.info("loggginfo.");
			}
		}));
	}

}
