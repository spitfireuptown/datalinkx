package com.datalinkx.dataserver.repository;

import com.datalinkx.dataserver.bean.domain.JobLogBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;


@Repository
public interface JobLogRepository extends CRUDRepository<JobLogBean, String> {


    String PAGE_QUERY_SQL = "select * from JOB_LOG where is_del = 0 and if(:jobId != '', job_id =:jobId, 1=1) order by start_time desc";
    @Query(
            value = PAGE_QUERY_SQL,
            countQuery = "SELECT COUNT(1) FROM (" + PAGE_QUERY_SQL + ") t",
            nativeQuery = true)
    Page<JobLogBean> pageQuery(Pageable pageable, String jobId);

    @Modifying
    @Transactional
    @Query(value = " update JobLogBean set isDel = 1 where jobId = :jobId and isDel = 0 ")
    void logicDeleteByJobId(String jobId);

    @Query(value = "select j from JobLogBean j where j.jobId = :jobId and j.startTime = :startTime")
    JobLogBean findByJobIdAndStartTime(String jobId, Timestamp startTime);

    List<JobLogBean> findAllByIsDel(Integer isDel);
}
