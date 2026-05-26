package com.datalinkx.dataserver.repository;

import com.datalinkx.dataserver.bean.domain.JobRelationBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * JobRelation 实体的仓库接口，用于与数据库进行交互。
 */
@Repository
public interface JobRelationRepository extends CrudRepository<JobRelationBean, String> {

    /**
     * 分页查询未删除的 JobRelation 记录，可根据 jobId 进行过滤。
     *
     * @param pageable 分页信息
     * @param jobId    作业 ID，可为空
     * @return 符合条件的 JobRelationBean 分页列表
     */
    @Query("SELECT j FROM JobRelationBean j WHERE j.isDel = 0 AND (:jobId = '' OR j.jobId = :jobId)")
    Page<JobRelationBean> pageQuery(Pageable pageable, String jobId);

    /**
     * 根据删除标记查询 JobRelation 记录。
     *
     * @param isDel 删除标记，0 表示未删除，1 表示已删除
     * @return 符合条件的 JobRelationBean 列表
     */
    List<JobRelationBean> findByIsDel(Integer isDel);

    /**
     * 根据关系 ID 逻辑删除 JobRelation 记录。
     *
     * @param relationId 关系 ID
     */
    @Modifying
    @Transactional
    @Query("UPDATE JobRelationBean j SET j.isDel = 1 WHERE j.relationId = :relationId AND j.isDel = 0")
    void logicDeleteByRelationId(String relationId);

    /**
     * 根据作业 ID 逻辑删除 JobRelation 记录。
     *
     * @param jobId 作业 ID
     */
    @Modifying
    @Transactional
    @Query("UPDATE JobRelationBean j SET j.isDel = 1 WHERE (j.jobId = :jobId OR j.subJobId = :jobId) AND j.isDel = 0")
    void logicDeleteByJobId(String jobId);

    @Query("SELECT j FROM JobRelationBean j WHERE j.isDel = 0 AND j.jobId = :jobId")
    List<JobRelationBean> findSubJob(String jobId);

    @Query("SELECT j FROM JobRelationBean j WHERE j.isDel = 0 AND j.subJobId = :jobId")
    List<JobRelationBean> findParentJob(String jobId);
}
