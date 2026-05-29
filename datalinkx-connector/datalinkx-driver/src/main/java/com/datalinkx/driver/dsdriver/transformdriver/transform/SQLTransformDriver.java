package com.datalinkx.driver.dsdriver.transformdriver.transform;

import com.datalinkx.common.constants.MetaConstants;
import com.datalinkx.common.utils.ObjectUtils;
import com.datalinkx.driver.dsdriver.DsDriverFactory;
import com.datalinkx.driver.dsdriver.IDsDriver;
import com.datalinkx.driver.dsdriver.base.meta.TransformNodeMeta;
import com.datalinkx.driver.dsdriver.base.transform.SQLNode;
import com.datalinkx.driver.dsdriver.base.transform.TransformNode;
import com.datalinkx.driver.dsdriver.transformdriver.ITransformDriver;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author: uptown
 * @date: 2024/11/17 17:33
*/
@Slf4j
public class SQLTransformDriver extends ITransformDriver {

    @Override
    public TransformNode transferInfo(Map<String, Object> commonSettings, String transferSQLMeta) {
        return SQLNode.builder()
                .query(transferSQLMeta)
                .sourceTableName(MetaConstants.CommonConstant.SOURCE_TABLE)
                .resultTableName(MetaConstants.CommonConstant.SQL_OUTPUT_TABLE)
                .pluginName(MetaConstants.CommonConstant.TRANSFORM_SQL)
                .build();
    }

    @Override
    public String analysisTransferMeta(JsonNode nodeMeta) {
        JsonNode dataMeta = nodeMeta.get("data");
        return dataMeta.get("sqlOperatorValue").asText();
    }

    @Override
    public TransformNodeMeta.ValidateResult verify(String connectId, JsonNode nodeMeta) {
        JsonNode dataMeta = findNodeDataByType(nodeMeta, "SQL");

        if (!dataMeta.has("sqlOperatorValue") || ObjectUtils.isEmpty(dataMeta.get("sqlOperatorValue"))) {
            return TransformNodeMeta.ValidateResult.builder().valid(false).message("SQL不能为空！").build();
        }

        String sql = dataMeta.get("sqlOperatorValue").asText();

        TransformNodeMeta.ValidateResult aliasCheckResult = checkFunctionAliases(sql);
        if (aliasCheckResult != null) {
            return aliasCheckResult;
        }

        try {
            IDsDriver driver = DsDriverFactory.getDriver(connectId);
            List<Map<String, Object>> result = driver.executeQuery(sql, true);
            List<TransformNodeMeta.OutputField> fieldResults = new ArrayList<>();
            if (!ObjectUtils.isEmpty(result)) {
                Map<String, Object> fieldName2Type = result.get(0);
                fieldName2Type.forEach((filedName, type) -> {
                    fieldResults.add(
                            TransformNodeMeta
                                    .OutputField
                                    .builder()
                                    .name(filedName)
                                    .type((String) type)
                                    .build()
                    );
                });
            }
            return TransformNodeMeta.ValidateResult.builder().outputFields(fieldResults).valid(true).build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return TransformNodeMeta.ValidateResult.builder().message("SQL驱动加载异常，请检查日志").valid(false).build();
        }
    }

    private TransformNodeMeta.ValidateResult checkFunctionAliases(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (!(statement instanceof Select)) {
                return null;
            }

            Select select = (Select) statement;
            PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
            
            @SuppressWarnings("unchecked")
            List<SelectItem> selectItems = plainSelect.getSelectItems();
            
            for (SelectItem item : selectItems) {
                if (item instanceof SelectExpressionItem) {
                    SelectExpressionItem exprItem = (SelectExpressionItem) item;
                    
                    if (exprItem.getExpression() instanceof Function) {
                        Function func = (Function) exprItem.getExpression();
                        String alias = exprItem.getAlias() != null ? exprItem.getAlias().getName() : null;
                        
                        if (alias == null || alias.isEmpty()) {
                            return TransformNodeMeta.ValidateResult.builder()
                                    .valid(false)
                                    .message("SQL函数 " + func.getName() + "() 必须使用别名，例如：AS xxx")
                                    .build();
                        }
                    }
                }
            }
        } catch (JSQLParserException e) {
            log.warn("SQL解析失败，使用默认校验逻辑: {}", e.getMessage());
            return null;
        }

        return null;
    }
}