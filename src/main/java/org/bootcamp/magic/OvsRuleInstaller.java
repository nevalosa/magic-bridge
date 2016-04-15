/*
 * Copyright 2015 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bootcamp.magic;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Set;

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficTreatment;
import org.slf4j.Logger;

/**
 * Populates rules for CORD VTN service.
 */
public class OvsRuleInstaller {

    protected final Logger log = getLogger(getClass());

    //Table 0
    private static final int TABLE_0 = 0;
    
    //Table 1
    private static final int TABLE_1 = 1;
    
    private static final int HIGH_PRIORITY = 65535;
    private final ApplicationId appId;
    private final FlowRuleService flowRuleService;

    /**
     * Creates a new rule populator.
     *
     * @param appId application id
     * @param flowRuleService flow rule service
     * @param deviceService device service
     * @param driverService driver service
     * @param groupService group service
     * @param tunnelType tunnel type
     */
    public OvsRuleInstaller(ApplicationId appId,
                                FlowRuleService flowRuleService,
                                DeviceService deviceService,
                                DriverService driverService,
                                String tunnelType) {
        this.appId = appId;
        this.flowRuleService = flowRuleService;
//        this.deviceService = deviceService;
    }

    /**
     * Installs table miss rule to a give device.
     *
     * @param deviceId device id to install the rules
     * @param dpIntf data plane interface name
     * @param dpIp data plane ip address
     */
    public void init(DeviceId deviceId, String dpIntf, IpAddress dpIp) {
    	
        defaultFlowRule(true);
    }
    
    /**
     * Installs or uninstall a default rule.
     *
     * @param install true to install, false to uninstall
     */
    private void defaultFlowRule(boolean install) {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .drop()
                .build();

        getVirtualSwitches().stream().forEach(deviceId -> {
            FlowRule flowRuleDirect = DefaultFlowRule.builder()
                    .fromApp(appId)
                    .withTreatment(treatment)
                    .withPriority(HIGH_PRIORITY)
                    .forDevice(deviceId)
                    .forTable(TABLE_0)
                    .makePermanent()
                    .build();

            processFlowRule(true, flowRuleDirect);
        });
    }
    
    private void table0FlowRule(){
    	
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setEthDst(MacAddress.NONE)
                .build();

        getVirtualSwitches().stream().forEach(deviceId -> {
            FlowRule flowRuleDirect = DefaultFlowRule.builder()
                    .fromApp(appId)
                    .withTreatment(treatment)
                    .withPriority(HIGH_PRIORITY)
                    .forDevice(deviceId)
                    .forTable(TABLE_0)
                    .makePermanent()
                    .build();

            processFlowRule(true, flowRuleDirect);
        });
    }
    
    
    private void table1FlowRule(){
    	
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
        		//TODO
//                .
                .build();

        getVirtualSwitches().stream().forEach(deviceId -> {
            FlowRule flowRuleDirect = DefaultFlowRule.builder()
                    .fromApp(appId)
                    .withTreatment(treatment)
                    .withPriority(HIGH_PRIORITY)
                    .forDevice(deviceId)
                    .forTable(TABLE_1)
                    .makePermanent()
                    .build();

            processFlowRule(true, flowRuleDirect);
        });
    }

    
    /**
     * Installs or uninstall a given rule.
     *
     * @param install true to install, false to uninstall
     * @param rule rule
     */
    private void processFlowRule(boolean install, FlowRule rule) {
        FlowRuleOperations.Builder oBuilder = FlowRuleOperations.builder();
        oBuilder = install ? oBuilder.add(rule) : oBuilder.remove(rule);

        flowRuleService.apply(oBuilder.build(new FlowRuleOperationsContext() {
            @Override
            public void onError(FlowRuleOperations ops) {
                log.error(String.format("Failed %s, %s", ops.toString(), rule.toString()));
            }
        }));
    }

    /**
     * Returns integration bridges configured in the system.
     *
     * @return set of device ids
     */
    private Set<DeviceId> getVirtualSwitches() {
//        CordVtnConfig config = configRegistry.getConfig(appId, CordVtnConfig.class);
//        if (config == null) {
//            log.debug("No configuration found for {}", appId.name());
//            return Sets.newHashSet();
//        }

//        return config.ovsNodes().stream().map(OvsNode::maigicBrId).collect(Collectors.toSet());
    	return null;
    }
 

    /**
     * Removes all rules related to a given service VM host.
     *
     * @param host host to be removed
     */
    public void removeBasicConnectionRules(Host host) {
        checkNotNull(host);

        DeviceId deviceId = host.location().deviceId();
        MacAddress mac = host.mac();
        PortNumber port = host.location().port();
        IpAddress ip = host.ipAddresses().stream().findFirst().orElse(null);

        for (FlowRule flowRule : flowRuleService.getFlowRulesById(appId)) {

//            IpPrefix dstIp = getDstIpFromSelector(flowRule);
//            if (dstIp != null && dstIp.equals(ip.toIpPrefix())) {
//                processFlowRule(false, flowRule);
//            }
        }

    }
}

