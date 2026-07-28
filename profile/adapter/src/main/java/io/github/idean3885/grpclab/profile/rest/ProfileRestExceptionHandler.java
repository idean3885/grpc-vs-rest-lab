package io.github.idean3885.grpclab.profile.rest;

import io.github.idean3885.grpclab.profile.exception.ProfileNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * REST 예외 번역.
 *
 * <p>같은 도메인 예외를 gRPC 어댑터는 {@code Status.NOT_FOUND} 로, 여기서는 HTTP 404 로 번역한다. 예외 번역 지점이 어댑터에 있어 도메인은
 * 두 프로토콜을 모르는 상태로 남는다.
 */
@RestControllerAdvice
public class ProfileRestExceptionHandler {

  @ExceptionHandler(ProfileNotFoundException.class)
  public ProblemDetail handleNotFound(ProfileNotFoundException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
  }
}
