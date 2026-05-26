package com.datalinkx.dataserver.config.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Set;

@Component
@Scope
@Aspect
public class LogicDeleteAspect {

    private static final ThreadLocal<Boolean> isProcessing = ThreadLocal.withInitial(() -> false);

    public LogicDeleteAspect() {
    }

    @Around("execution(* javax.persistence.EntityManager.createQuery(javax.persistence.criteria.CriteriaQuery))")
    public Object createQueryx(ProceedingJoinPoint pjp) throws Throwable {
        if (isProcessing.get()) {
            return pjp.proceed();
        }
        
        try {
            isProcessing.set(true);
            CriteriaQuery<?> cq = (CriteriaQuery)pjp.getArgs()[0];
            Predicate pd = cq.getRestriction();
            Set<Root<?>> roots = cq.getRoots();
            Root<?> root;
            if (roots.isEmpty()) {
                root = cq.from(cq.getSelection().getJavaType());
            } else {
                root = roots.iterator().next();
            }

            EntityManager em = (EntityManager)pjp.getTarget();
            Predicate pd2 = em.getCriteriaBuilder().equal(root.get("isDel"), 0);
            cq.where(pd, pd2);
            return pjp.proceed();
        } finally {
            isProcessing.set(false);
        }
    }

    @Around("execution(* javax.persistence.EntityManager.createQuery(..))")
    public Object createQuery(ProceedingJoinPoint pjp) throws Throwable {
        if (isProcessing.get()) {
            return pjp.proceed();
        }
        
        try {
            isProcessing.set(true);
            Object[] args = pjp.getArgs();
            if (args[0] instanceof String) {
                String jpql = String.valueOf(args[0]);
                if (jpql.contains("isDel")) {
                    return pjp.proceed();
                } else {
                    String modifiedJpql;
                    if (jpql.toUpperCase().contains("WHERE")) {
                        modifiedJpql = jpql + " AND isDel=0";
                    } else {
                        modifiedJpql = jpql + " WHERE isDel=0";
                    }
                    args[0] = modifiedJpql;
                    return pjp.proceed(args);
                }
            } else {
                return pjp.proceed();
            }
        } finally {
            isProcessing.set(false);
        }
    }

    @Around("execution(* javax.persistence.EntityManager.createNativeQuery(..))")
    public Object createNativeQuery(ProceedingJoinPoint pjp) throws Throwable {
        if (isProcessing.get()) {
            return pjp.proceed();
        }
        
        try {
            isProcessing.set(true);
            Object[] args = pjp.getArgs();
            String nativeSQL = String.valueOf(args[0]);
            if (nativeSQL.contains("is_del")) {
                return pjp.proceed();
            } else {
                String modifiedSQL;
                if (nativeSQL.toUpperCase().contains("WHERE")) {
                    modifiedSQL = nativeSQL + " AND is_del=0";
                } else {
                    modifiedSQL = nativeSQL + " WHERE is_del=0";
                }
                args[0] = modifiedSQL;
                return pjp.proceed(args);
            }
        } finally {
            isProcessing.set(false);
        }
    }
}
