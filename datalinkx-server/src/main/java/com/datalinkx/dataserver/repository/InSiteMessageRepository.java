package com.datalinkx.dataserver.repository;

import com.datalinkx.dataserver.bean.domain.InSiteMessageBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface InSiteMessageRepository extends CRUDRepository<InSiteMessageBean, Long> {
    Optional<InSiteMessageBean> findById(Long id);

    @Query("select a from InSiteMessageBean a where a.userId = :userId AND a.isDel = 0")
    Page<InSiteMessageBean> pageQuery(Pageable pageable, String userId);
    List<InSiteMessageBean> findByUserIdAndIsDel(String userId, Integer isDel);
    List<InSiteMessageBean> findByUserIdAndReadAndIsDel(String userId, Boolean read, Integer isDel);
    
    @Modifying
    @Transactional
    @Query("update InSiteMessageBean m set m.read = :read where m.id = :id")
    void updateIsReadById(Long id, Boolean read);
    
    @Modifying
    @Transactional
    @Query("update InSiteMessageBean m set m.read = :read where m.userId = :userId")
    void updateIsReadByUserId(String userId, Boolean read);
    
    @Modifying
    @Transactional
    @Query("update InSiteMessageBean m set m.isDel = 1 where m.id = :id")
    void logicDeleteById(Long id);
}
