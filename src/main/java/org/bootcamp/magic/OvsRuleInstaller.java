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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onlab.packet.VlanId;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultGroupId;
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.ExtensionTreatmentResolver;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DefaultDriverData;
import org.onosproject.net.driver.DefaultDriverHandler;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.instructions.ExtensionPropertyException;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModEtherInstruction;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.group.GroupService;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.flow.criteria.Criterion.Type.IN_PORT;
import static org.onosproject.net.flow.criteria.Criterion.Type.IPV4_DST;
import static org.onosproject.net.flow.criteria.Criterion.Type.IPV4_SRC;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_TUNNEL_DST;
import static org.onosproject.net.flow.instructions.L2ModificationInstruction.L2SubType.ETH_DST;
import static org.onosproject.net.flow.instructions.L2ModificationInstruction.L2SubType.VLAN_PUSH;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Populates rules for CORD VTN service.
 */
public class OvsRuleInstaller {

    protected final Logger log = getLogger(getClass());

    private static final int TABLE_FIRST = 0;
    private static final int TABLE_IN_PORT = 1;
    private static final int TABLE_ACCESS_TYPE = 2;
    private static final int TABLE_IN_SERVICE = 3;
    private static final int TABLE_DST_IP = 4;
    private static final int TABLE_TUNNEL_IN = 5;

    private static final int HIGH_PRIORITY = 65535;
    private static final int DEFAULT_PRIORITY = 0;
    private static final int LOWEST_PRIORITY = 0;

    private static final VlanId VLAN_WAN = VlanId.vlanId((short) 500);

    private static final String PORT_NAME = "portName";
    private static final String DATA_PLANE_INTF = "dataPlaneIntf";
    private static final String S_TAG = "stag";

    private final ApplicationId appId;
    private final FlowRuleService flowRuleService;
    private final DeviceService deviceService;
    private final DriverService driverService;
    private final GroupService groupService;
    private final NetworkConfigRegistry configRegistry;

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
                                GroupService groupService,
                                NetworkConfigRegistry configRegistry,
                                String tunnelType) {
        this.appId = appId;
        this.flowRuleService = flowRuleService;
        this.deviceService = deviceService;
        this.driverService = driverService;
        this.groupService = groupService;
        this.configRegistry = configRegistry;
    }

    /**
     * Installs table miss rule to a give device.
     *
     * @param deviceId device id to install the rules
     * @param dpIntf data plane interface name
     * @param dpIp data plane ip address
     */
    public void init(DeviceId deviceId, String dpIntf, IpAddress dpIp) {

        defaultFlowRule();
    }
    
    /**
     * Populates drop rules that does not match any direct access rules but has
     * destination to a different service network in ACCESS_TYPE table.
     *
     * @param dstRange destination ip range
     */
    private void defaultFlowRule() {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .drop()
                .build();

        getVirtualSwitches().stream().forEach(deviceId -> {
            FlowRule flowRuleDirect = DefaultFlowRule.builder()
                    .fromApp(appId)
                    .withTreatment(treatment)
                    .withPriority(HIGH_PRIORITY)
                    .forDevice(deviceId)
                    .forTable(TABLE_FIRST)
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
     * Flush flows installed by this application.
     */
    public void flushRules() {
        flowRuleService.getFlowRulesById(appId).forEach(flowRule -> processFlowRule(false, flowRule));
    }

    /**
     * Returns integration bridges configured in the system.
     *
     * @return set of device ids
     */
    private Set<DeviceId> getVirtualSwitches() {
        CordVtnConfig config = configRegistry.getConfig(appId, CordVtnConfig.class);
        if (config == null) {
            log.debug("No configuration found for {}", appId.name());
            return Sets.newHashSet();
        }

        return config.ovsNodes().stream()
                .map(OvsNode::intBrId).collect(Collectors.toSet());
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

//    /**
//     * Removes service dependency rules.
//     *
//     * @param tService tenant cord service
//     * @param pService provider cord service
//     */
//    public void removeServiceDependencyRules(CordService tService, CordService pService) {
//        checkNotNull(tService);
//        checkNotNull(pService);
//
//        Ip4Prefix srcRange = tService.serviceIpRange().getIp4Prefix();
//        Ip4Prefix dstRange = pService.serviceIpRange().getIp4Prefix();
//        IpPrefix serviceIp = pService.serviceIp().toIpPrefix();
//
//        Map<DeviceId, GroupId> outGroups = Maps.newHashMap();
//        GroupKey groupKey = new DefaultGroupKey(pService.id().id().getBytes());
//
//        getVirtualSwitches().stream().forEach(deviceId -> {
//            Group group = groupService.getGroup(deviceId, groupKey);
//            if (group != null) {
//                outGroups.put(deviceId, group.id());
//            }
//        });
//
//        for (FlowRule flowRule : flowRuleService.getFlowRulesById(appId)) {
//            IpPrefix dstIp = getDstIpFromSelector(flowRule);
//            IpPrefix srcIp = getSrcIpFromSelector(flowRule);
//
//            if (dstIp != null && dstIp.equals(serviceIp)) {
//                processFlowRule(false, flowRule);
//                continue;
//            }
//
//            if (dstIp != null && srcIp != null) {
//                if (dstIp.equals(dstRange) && srcIp.equals(srcRange)) {
//                    processFlowRule(false, flowRule);
//                    continue;
//                }
//
//                if (dstIp.equals(srcRange) && srcIp.equals(dstRange)) {
//                    processFlowRule(false, flowRule);
//                    continue;
//                }
//            }
//
//            GroupId groupId = getGroupIdFromTreatment(flowRule);
//            if (groupId != null && groupId.equals(outGroups.get(flowRule.deviceId()))) {
//                processFlowRule(false, flowRule);
//            }
//        }
//
//        // TODO remove the group if it is not in use
//    }

    /**
     * Returns integration bridges configured in the system.
     *
     * @return set of device ids
     */
//    private Set<DeviceId> getVirtualSwitches() {
//        CordVtnConfig config = configRegistry.getConfig(appId, CordVtnConfig.class);
//        if (config == null) {
//            log.debug("No configuration found for {}", appId.name());
//            return Sets.newHashSet();
//        }
//
//        return config.cordVtnNodes().stream()
//                .map(CordVtnNode::intBrId).collect(Collectors.toSet());
//    }
}

