package com.bwdesigngroup.ignition.project_scan.common;

import com.inductiveautomation.ignition.common.rpc.RpcInterface;
import com.bwdesigngroup.ignition.project_scan.common.ProjectScanConstants;

@RpcInterface(packageId = ProjectScanConstants.MODULE_ID)
public interface ProjectScanRPC {
    /**
     * Triggers a project scan on the gateway
     * @param updateDesigners Whether to notify designers to update
     * @param forceUpdate Whether to force the update in designers
     * @return JSON string containing result of scan
     */
    String scanProject(Boolean updateDesigners, Boolean forceUpdate);
}