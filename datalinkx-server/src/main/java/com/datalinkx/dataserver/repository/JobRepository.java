package com.datalinkx.dataserver.repository;

import com.datalinkx.dataserver.bean.domain.JobBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@Repository
public interface JobRepository extends CRUDRepository<JobBean, String> {

	@Query("select j from JobBean j where j.jobId = :jobId")
	Optional<JobBean> findByJobId(String jobId);

	Optional<JobBean> findByName(String name);

	List<JobBean> findByJobIdIn(List<String> jobIds);

	@Query("select j from JobBean j where j.type = :type and j.retryTime < 5 and j.isDel = 0")
	List<JobBean> findRestartJob(Integer type);


	@Query("select j from JobBean j where (j.readerDsId = :jobId or j.writerDsId = :jobId)")
	List<JobBean> findDependJobId(String jobId);

	@Query("select j from JobBean j where j.isDel = 0")
	List<JobBean> findAll();

	@Modifying
	@Transactional
	@Query("update JobBean j set j.status = :status where j.jobId = :jobId")
	void updateJobStatus(String jobId, Integer status);


	@Modifying
	@Transactional
	@Query("update JobBean j set j.retryTime = j.retryTime + 1 where j.jobId = :jobId")
	void addRetryTime(String jobId);

	@Modifying
	@Transactional
	@Query("update JobBean j set j.isDel = 1 where j.jobId = :jobId")
	void logicDeleteByJobId(String jobId);


	@Query(value = "select * from JOB where if(:type != -1, type =:type, 1=1) AND is_del = 0", nativeQuery = true)
	Page<JobBean> pageQuery(Pageable pageable, Integer type);
}
