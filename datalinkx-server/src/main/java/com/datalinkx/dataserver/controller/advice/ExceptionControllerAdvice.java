package com.datalinkx.dataserver.controller.advice;


import com.datalinkx.common.result.WebResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;



@Slf4j
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ExceptionControllerAdvice {

    @ResponseBody
    @ExceptionHandler(value = {IllegalStateException.class, IllegalArgumentException.class})
    public WebResult<?> handleException(Exception exception) throws Exception {
        return WebResult.fail(exception, null);
    }

    @ResponseBody
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public WebResult<?> handleException(MethodArgumentNotValidException exception) {
        return WebResult.fail(exception, ErrorsUtils.compositeValiditionError(exception.getBindingResult()));
    }


    @ResponseBody
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public WebResult<?> handleException(MissingServletRequestParameterException exception) throws Exception {
        return WebResult.fail(exception, exception.getParameterName());
    }



    /**
     * jpa层的校验
     *
     * @param exception
     * @return
     * @throws Exception
     */
    @ExceptionHandler(value = {ConstraintViolationException.class})
    public WebResult<?> validationException(ConstraintViolationException exception) throws Exception {
        return WebResult.fail(exception,
                exception.getConstraintViolations()
                        .stream()
                        .collect(Collectors.toMap(
                                ConstraintViolation::getPropertyPath,
                                ConstraintViolation::getMessage,
                                (m1, m2) -> m1 + m2))
        );
    }


    /**
     * 统一处理Controller中的异常 需要统一处理异常编码
     *
     * @param runtimeException
     * @return
     */
    @ExceptionHandler({RuntimeException.class})
    @ResponseBody
    public ResponseEntity<?> runtimeExceptionHandler(RuntimeException runtimeException) {
        Throwable r = ErrorsUtils.getRootCause(runtimeException);
        WebResult result = WebResult.fail(r);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body(result);
    }


    @ResponseBody
    @ExceptionHandler(value = {Exception.class, Error.class})
    public WebResult<?> handleUncatchException(Throwable exception) {
        return WebResult.fail(exception, null);
    }
}
