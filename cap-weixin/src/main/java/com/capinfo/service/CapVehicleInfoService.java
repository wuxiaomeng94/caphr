package com.capinfo.service;

import com.capinfo.base.BaseMapper;
import com.capinfo.base.CurrentUser;
import com.capinfo.base.impl.BaseServiceImpl;
import com.capinfo.entity.CapVehicleInfo;
import com.capinfo.entity.CapWorkOrderRecord;
import com.capinfo.entity.CapWxAccountFans;
import com.capinfo.exception.MyException;
import com.capinfo.mapper.CapVehicleInfoMapper;
import com.capinfo.mapper.CapWxAccountFansMapper;
import com.capinfo.util.ReType;
import com.capinfo.vehicle.utilEntity.VehicleConstant;
import com.capinfo.vehicle.utilEntity.VehicleFlowEntity;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CapVehicleInfoService extends BaseServiceImpl<CapVehicleInfo, String> {

    @Autowired
    private CapVehicleInfoMapper capVehicleInfoMapper;





    @Override
    public BaseMapper<CapVehicleInfo, String> getMappser() {
        return capVehicleInfoMapper;
    }


    public ReType showJoinOrderRecord(CapVehicleInfo capVehicleInfo, int page, int limit) {
        List<CapVehicleInfo> tList = null;
        Page<CapVehicleInfo> tPage = PageHelper.startPage(page, limit);
        try {
            tList = capVehicleInfoMapper.selectListByCondition(capVehicleInfo);
        } catch (MyException e) {
            //log.error("class:BaseServiceImpl ->method:show->message:" + e.getMessage());
            e.printStackTrace();
        }
        return new ReType(tPage.getTotal(), tList);
    }


    /**
     * 根据条件查询
     * @param capVehicleInfo
     * @return
     */
    public List<CapVehicleInfo> selectListByCondition(CapVehicleInfo capVehicleInfo) {
        return capVehicleInfoMapper.selectListByCondition(capVehicleInfo);
    }





    public CapVehicleInfo save(CapVehicleInfo capVehicleInfo) {
        CurrentUser currentUser = (CurrentUser) SecurityUtils.getSubject().getSession().getAttribute("curentUser");
        String id = capVehicleInfo.getId();
        if (StringUtils.isBlank(id)) {
            capVehicleInfo.setId(UUID.randomUUID().toString().replaceAll("-", ""));
            //因为是模拟摄像头拍下车牌后记录数据，createby写一个默认的用户
            capVehicleInfo.setCreateBy(VehicleConstant.USER_WORKER_ID);
            capVehicleInfo.setCreateDate(new Date());
            capVehicleInfo.setUpdateBy(VehicleConstant.USER_WORKER_ID);
            capVehicleInfo.setUpdateDate(new Date());
            capVehicleInfo.setDelFlag("0");
            this.insertSelective(capVehicleInfo);
        } else {
            capVehicleInfo.setUpdateBy(currentUser.getId());
            capVehicleInfo.setUpdateDate(new Date());
            this.updateByPrimaryKey(capVehicleInfo);
        }
        return capVehicleInfo;
    }





    /**
     * 需要这么一个东西，判断接下来走哪一步
     * @param status
     * @param capWorkOrderRecord
     * @return
     */
    public VehicleFlowEntity getMatchMap(String status, CapWorkOrderRecord capWorkOrderRecord) {
        VehicleFlowEntity flow = new VehicleFlowEntity();
        Map<String, Object> map = new HashMap<String, Object>();
        //数据进来的时候是这个节点状态。出去需要改变成下一个节点对应的
        String nowLink = capWorkOrderRecord.getNowLink();
        switch (nowLink) {
            case VehicleConstant.PROCESS_APPEAR:
                //外观检测
                if ("pass".equals(status)) {
                    //下一步到尾气检测 ，可能需要判断是否是新能源汽车
                    map.put("pass", "1");
                    flow.setNowLink(VehicleConstant.PROCESS_GAS);
                } else if ("nopass".equals(status)) {
                    map.put("pass", "2");
                    flow.setNowLink(VehicleConstant.PROCESS_APPEAR);
                    flow.setNowStatus(VehicleConstant.PROCESS_NOWSTATUS_NO);
                }
                flow.setStepMoney(10);
                break;
            case VehicleConstant.PROCESS_GAS:
                //尾气检测
                if ("pass".equals(status)) {
                    map.put("pass", "1");
                    flow.setNowLink(VehicleConstant.PROCESS_ONLINE);
                } else if ("nopass".equals(status)) {
                    map.put("pass", "2");
                    flow.setNowLink(VehicleConstant.PROCESS_GAS);
                    flow.setNowStatus(VehicleConstant.PROCESS_NOWSTATUS_NO);
                }
                flow.setStepMoney(20);
                break;
            case VehicleConstant.PROCESS_ONLINE:
                //上线检测
                if ("pass".equals(status)) {
                    map.put("pass", "1");
                    flow.setNowLink(VehicleConstant.PROCESS_PAY);
                } else if ("nopass".equals(status)) {
                    map.put("pass", "2");
                    flow.setNowLink(VehicleConstant.PROCESS_ONLINE);
                    flow.setNowStatus(VehicleConstant.PROCESS_NOWSTATUS_NO);
                    flow.setStepMoney(20);
                } else if ("nopasslight".equals(status)) {
                    map.put("pass", "3");
                    flow.setNowLink(VehicleConstant.PORCESS_LIGHT);
                }
                flow.setStepMoney(20);
                break;
            case VehicleConstant.PORCESS_LIGHT:
                //车灯复检
                if ("pass".equals(status)) {
                    map.put("pass", "1");
                    flow.setNowLink(VehicleConstant.PROCESS_PAY);
                } else if ("nopass".equals(status)) {
                    map.put("pass", "2");
                    flow.setNowLink(VehicleConstant.PORCESS_LIGHT);
                }
                flow.setStepMoney(20);
                break;
        }
        flow.setMap(map);
        return flow;
    }



}
