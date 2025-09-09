package com.datalinkx.common.result;

import static com.datalinkx.common.constants.MetaConstants.CommonConstant.TRACE_ID;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
@Data
public class WebResult<T> {
	private String status;
	private String errstr;
	private String traceId;
	@Setter
    private T result;


	public WebResult() {
	}


    public WebResult<T> setStatus(String status) {
		this.status = status;
		this.traceId = MDC.get(TRACE_ID);
		return this;
	}

	public WebResult<T> setErrstr(String errstr) {
		this.errstr = errstr;
		return this;
	}

	public static <T> WebResult<T> of(T result) {
		if (result instanceof Optional) {
			return of((Optional<T>) result);
		}
		WebResult<T> r = new WebResult<>();;
		r.setResult(result);
		r.setStatus(String.valueOf(StatusCode.SUCCESS.getValue()));
		return r;
	}

	public static <T> WebResult<T> of(Optional<T> result) {
		return result.isPresent()
				? of(result.get())
				: new WebResult<T>().setErrstr("object does not exists").setStatus("101");
	}

	public static <T> WebResult<T> fail(Throwable throwable) {
		log.error(throwable.getMessage(), throwable);
		WebResult<T> r = new WebResult<>();
		r.setStatus(String.valueOf(StatusCode.API_INTERNAL_ERROR.getValue()));
		r.setErrstr(throwable.getMessage());
		return r;
	}

	public static <T> WebResult<T> fail(Throwable throwable, T o) {
		log.error(throwable.getMessage(), throwable);
		String msg = Optional.ofNullable(throwable.getCause())
				.map(s -> s.getMessage() + ",")
				.orElse("") + throwable.getMessage();
		WebResult<T> r = new WebResult<>();
		r.setErrstr(msg);
		r.setStatus(String.valueOf(StatusCode.API_INTERNAL_ERROR.getValue()));
		r.setResult(o);
		return r;
	}
}
