package com.jianli.sys.workflow.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jianli.common.dao.DaoUtil;
import com.jianli.common.dao.QueryCondition;
import com.jianli.common.redis.RedisUtil;
import com.jianli.common.service.BaseService;
import com.jianli.common.service.JsonRequest;
import com.jianli.common.service.JsonResponse;
import com.jianli.common.util.SecurityUtil;
import com.jianli.sys.dao.lookup.UserLookup;
import com.jianli.sys.service.SysCodeService;
import com.jianli.sys.workflow.domain.WorkflowInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;


@Service("workflowMonitor")
public class WorkflowMonitorService extends BaseService {

    @Autowired
    private DaoUtil daoUtil;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private UserLookup userLookup;
    @Autowired
    SysCodeService sysCodeService;


    public JsonResponse list(JsonRequest jsonRequest) throws Exception {
        authentication(jsonRequest);

        List<QueryCondition> conditions = daoUtil.getConditions(jsonRequest.getData());

        JsonResponse response = JsonResponse.create(jsonRequest);

        JSONObject data = daoUtil.page(createListSQL("SELECT workflow_instance.id, workflow_instance.instanceCode, workflow_instance.workflowId, workflow.workflowName, workflow.instanceUrl,workflow_instance.createBy, workflow_instance.createTime, workflow_instance.finishTime, workflow_instance.status AS instanceStatus, workflow_step.stepName, GROUP_CONCAT(DISTINCT sys_user.userName) AS activityUsers FROM workflow_instance LEFT JOIN workflow ON workflow.id = workflow_instance.workflowId LEFT JOIN workflow_activity ON workflow_instance.id = workflow_activity.instanceId AND workflow_instance.flowId = workflow_activity.flowId LEFT JOIN workflow_step ON workflow_step.id = workflow_activity.stepId LEFT JOIN sys_user ON sys_user.id = workflow_activity.userId GROUP BY workflow_instance.id, workflow_instance.status, workflow_step.stepName"),
                conditions,  daoUtil.getPageInfo(jsonRequest.getData(), "id DESC"));

        JSONArray entityList = data.getJSONArray("entityList");

        for (int i = 0; i < entityList.size(); i++) {
            JSONObject entity = entityList.getJSONObject(i);
            if(entity.getInteger("instanceStatus") != null && entity.getInteger("instanceStatus").equals(WorkflowInstance.Status.Completed))
            {
                entity.put("activityUsers", "");
            }
        }

        data.put("instanceStatus", sysCodeService.getCodeList("workflowInstanceStatus") );

        userLookup.fillCodeTables(data, entityList, "createBy");

        response.setData(data);

        return response;
    }



}
