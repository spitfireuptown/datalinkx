
package com.datalinkx.datajob.action;

import com.datalinkx.common.result.DatalinkXJobDetail;
import com.datalinkx.common.utils.IdUtils;
import lombok.extern.slf4j.Slf4j;

import static com.datalinkx.common.constants.MetaConstants.JobStatus.*;


@Slf4j
public abstract class AbstractDataTransferAction<T extends DatalinkXJobDetail, U> {
    protected abstract void begin(T info);
    protected abstract void end(U unit, int status, String errmsg);
    protected abstract void beforeExec(U unit) throws Exception;
    protected abstract void execute(U unit) throws Exception;
    protected abstract boolean checkResult(U unit);
    protected abstract void afterExec(U unit, boolean success);
    protected abstract U convertExecUnit(T info) throws Exception;
    protected abstract void destroyed(U unit, int status, String errmsg);

    public void doAction(T actionInfo) throws Exception {
        Thread taskCheckerThread;
        // T -> U 获取引擎执行类对象
        U execUnit = convertExecUnit(actionInfo);
        int status = JOB_STATUS_SUCCESS;
        StringBuffer error = new StringBuffer();
        try {
            // 1、准备执行job
            this.begin(actionInfo);

            // 2、获取任务健康检查线程名称
            String healthCheck = IdUtils.getHealthThreadName(actionInfo.getJobId(), actionInfo.getType());

            // 3、循环检查任务结果
            taskCheckerThread = new Thread(() -> {
                while (true) {
                    try {
                        // 3.1、如果任务执行完成
                        if (checkResult(execUnit)) {
                            // 3.2、执行任务后置处理钩子
                            this.afterExec(execUnit, true);
                            break;
                        }
                        Thread.sleep(5000);
                    } catch (Exception e) {
                        log.error("datalinkx job error ", e);
                        String errorMsg = e.getMessage();
                        error.append(errorMsg).append("\r\n");
                        log.info(errorMsg);
                        this.afterExec(execUnit, false);
                        break;
                    }
                }
            }, healthCheck);

            // 4.1、每个单元执行前的准备
            this.beforeExec(execUnit);
            // 4.2、向引擎提交任务
            this.execute(execUnit);

            // 阻塞至任务完成
            taskCheckerThread.start();
            taskCheckerThread.join();
            if (error.length() != 0) {
                // 用户手动取消的任务把状态置为停止
                if (error.toString().contains("data-transfer task canceled")) {

                    status = JOB_STATUS_STOP;
                } else {

                    status = JOB_STATUS_ERROR;
                }
            }

            // 5、整个Job结束后的处理
            this.end(execUnit, status, error.toString());
        } catch (Throwable e) {
            log.error("datalinkx job failed -> ", e);
            status = JOB_STATUS_ERROR;
            error.append(e.getMessage()).append("\r\n");
            this.end(execUnit, status, error.toString());
        }

        // 6、结束后的钩子处理
        this.destroyed(execUnit, status, error.toString());
    }
}
