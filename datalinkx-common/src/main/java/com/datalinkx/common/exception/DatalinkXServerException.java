package com.datalinkx.common.exception;

import java.util.Map;

import com.datalinkx.common.result.StatusCode;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;


@Getter
public class DatalinkXServerException extends RuntimeException {
	private static final long serialVersionUID = -940285811464169752L;

	private StatusCode status;


	public DatalinkXServerException(String msg) {
		super(msg);
		status = StatusCode.API_INTERNAL_ERROR;
	}

	public DatalinkXServerException(StatusCode status, String msg) {
		super(msg);
		this.status = status;
	}

	public DatalinkXServerException(StatusCode status) {
		super(status.getMsg());
		this.status = status;
	}

	public DatalinkXServerException(Throwable throwable) {
		super(throwable);
		status = StatusCode.API_INTERNAL_ERROR;
	}

	public DatalinkXServerException(String msg, Throwable throwable) {
		super(msg, throwable);
		status = StatusCode.API_INTERNAL_ERROR;
	}

}
