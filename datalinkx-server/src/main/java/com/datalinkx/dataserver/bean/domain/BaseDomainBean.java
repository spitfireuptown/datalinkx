package com.datalinkx.dataserver.bean.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;
import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
@MappedSuperclass
@SuperBuilder(toBuilder = true)
public class BaseDomainBean {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "ctime")
    private Timestamp ctime;
    @Column(name = "utime")
    private Timestamp utime;
    @Column(name = "is_del")
    private Integer isDel = 0;
}
