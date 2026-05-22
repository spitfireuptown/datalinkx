package com.datalinkx.dataserver.repository;

import com.datalinkx.dataserver.bean.domain.SystemParamBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemParamRepository extends CRUDRepository<SystemParamBean, Long> {

	Optional<SystemParamBean> findById(Long id);

	Optional<SystemParamBean> findByParamKeyAndParamScope(String paramKey, String paramScope);

	List<SystemParamBean> findAllByIsDel(Integer isDel);

	List<SystemParamBean> findAllByParamScopeAndIsDel(String paramScope, Integer isDel);

	@Query(value = " select * from SYSTEM_PARAM where is_del = 0 "
			+ " and if(:paramKey != '', param_key like concat('%', :paramKey, '%'), 1=1) "
			+ " and if(:paramScope != '', param_scope = :paramScope, 1=1) ", nativeQuery = true)
	Page<SystemParamBean> pageQuery(Pageable pageable, String paramKey, String paramScope);

	@Transactional
	@Modifying
	@Query("update SystemParamBean p set p.isDel = 1 where p.id = :id")
	void deleteById(Long id);
}