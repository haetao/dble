package com.actiontech.dble.backend.datasource.check;

import com.actiontech.dble.alarm.AlarmCode;
import com.actiontech.dble.alarm.Alert;
import com.actiontech.dble.alarm.AlertUtil;
import com.actiontech.dble.alarm.ToResolveContainer;
import com.actiontech.dble.sqlengine.SQLQueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Created by szf on 2019/12/24.
 */
public class CountChecker extends AbstractConsistencyChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(CountChecker.class);

    @Override
    String[] getFetchCols() {
        return new String[]{"cr"};
    }

    @Override
    String getCountSQL(String dbName, String tName) {
        return "select count(1) as cr from " + tName;
    }

    @Override
    boolean resultEquals(SQLQueryResult<List<Map<String, String>>> or, SQLQueryResult<List<Map<String, String>>> cr) {
        Map<String, String> oresult = or.getResult().get(0);
        Map<String, String> cresult = cr.getResult().get(0);
        return oresult.get("cr").equals(cresult.get("cr"));
    }

    @Override
    void failResponse(List<SQLQueryResult<List<Map<String, String>>>> res) {
        String tableId = schema + "." + tableName;
        String errorMsg = "Global Consistency Check fail for table :" + schema + "-" + tableName;
        LOGGER.info(errorMsg);
        for (SQLQueryResult<List<Map<String, String>>> r : res) {
            LOGGER.info("Checksum is : " + r.getResult().get(0).get("cr"));
        }
        AlertUtil.alertSelf(AlarmCode.GLOBAL_TABLE_COLUMN_LOST, Alert.AlertLevel.WARN, errorMsg, AlertUtil.genSingleLabel("TABLE", tableId));
        ToResolveContainer.GLOBAL_TABLE_CONSISTENCY.add(tableId);
    }

    @Override
    void successResponse(List<SQLQueryResult<List<Map<String, String>>>> elist) {
        String tableId = schema + "." + tableName;
        LOGGER.info("Global Consistency Check success for table :" + schema + "-" + tableName);
        for (SQLQueryResult<List<Map<String, String>>> r : elist) {
            LOGGER.info("error node is : " + r.getTableName() + "-" + r.getDataNode());
        }
        if (ToResolveContainer.GLOBAL_TABLE_CONSISTENCY.contains(tableId)) {
            AlertUtil.alertSelfResolve(AlarmCode.GLOBAL_TABLE_COLUMN_LOST, Alert.AlertLevel.WARN, AlertUtil.genSingleLabel("TABLE", tableId),
                    ToResolveContainer.GLOBAL_TABLE_CONSISTENCY, tableId);
        }
    }
}
