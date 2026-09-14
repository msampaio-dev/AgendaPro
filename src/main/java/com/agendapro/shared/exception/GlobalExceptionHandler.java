package com.agendapro.shared.exception;

import java.time.Instant;
import java.util.List;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErroApiResponse> tratarValidacaoCorpo(
			MethodArgumentNotValidException exception,
			HttpServletRequest request
	) {
		List<CampoInvalidoResponse> campos = exception.getBindingResult()
				.getFieldErrors()
				.stream()
				.map(erro -> new CampoInvalidoResponse(
						erro.getField(),
						erro.getDefaultMessage()
				))
				.toList();
		return resposta(HttpStatus.BAD_REQUEST, "Dados inválidos", request, campos);
	}

	@ExceptionHandler(BindException.class)
	public ResponseEntity<ErroApiResponse> tratarValidacaoParametros(
			BindException exception,
			HttpServletRequest request
	) {
		List<CampoInvalidoResponse> campos = exception.getFieldErrors()
				.stream()
				.map(erro -> new CampoInvalidoResponse(
						erro.getField(),
						erro.getDefaultMessage()
				))
				.toList();
		return resposta(HttpStatus.BAD_REQUEST, "Parâmetros inválidos", request, campos);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErroApiResponse> tratarRestricoes(
			ConstraintViolationException exception,
			HttpServletRequest request
	) {
		List<CampoInvalidoResponse> campos = exception.getConstraintViolations()
				.stream()
				.map(violacao -> new CampoInvalidoResponse(
						violacao.getPropertyPath().toString(),
						violacao.getMessage()
				))
				.toList();
		return resposta(HttpStatus.BAD_REQUEST, "Parâmetros inválidos", request, campos);
	}

	@ExceptionHandler({
			HttpMessageNotReadableException.class,
			MethodArgumentTypeMismatchException.class
	})
	public ResponseEntity<ErroApiResponse> tratarFormatoInvalido(
			Exception exception,
			HttpServletRequest request
	) {
		return resposta(HttpStatus.BAD_REQUEST, "Formato de dados inválido", request, List.of());
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErroApiResponse> tratarRotaInexistente(
			NoResourceFoundException exception,
			HttpServletRequest request
	) {
		return resposta(HttpStatus.NOT_FOUND, "Recurso não encontrado", request, List.of());
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErroApiResponse> tratarAcessoNegado(
			AccessDeniedException exception,
			HttpServletRequest request
	) {
		return resposta(HttpStatus.FORBIDDEN, "Acesso negado", request, List.of());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErroApiResponse> tratarIntegridade(
			DataIntegrityViolationException exception,
			HttpServletRequest request
	) {
		return resposta(
				HttpStatus.CONFLICT,
				"A operação conflita com os dados existentes",
				request,
				List.of()
		);
	}

	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<ErroApiResponse> tratarRegraDeNegocio(
			RuntimeException exception,
			HttpServletRequest request
	) {
		ResponseStatus responseStatus = AnnotatedElementUtils.findMergedAnnotation(
				exception.getClass(),
				ResponseStatus.class
		);
		if (responseStatus == null) {
			return resposta(
					HttpStatus.INTERNAL_SERVER_ERROR,
					"Ocorreu um erro interno",
					request,
					List.of()
			);
		}

		return resposta(responseStatus.code(), exception.getMessage(), request, List.of());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErroApiResponse> tratarErroInesperado(
			Exception exception,
			HttpServletRequest request
	) {
		return resposta(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"Ocorreu um erro interno",
				request,
				List.of()
		);
	}

	private ResponseEntity<ErroApiResponse> resposta(
			HttpStatus status,
			String mensagem,
			HttpServletRequest request,
			List<CampoInvalidoResponse> campos
	) {
		return ResponseEntity.status(status).body(new ErroApiResponse(
				Instant.now(),
				status.value(),
				status.getReasonPhrase(),
				mensagem,
				request.getRequestURI(),
				campos
		));
	}
}
