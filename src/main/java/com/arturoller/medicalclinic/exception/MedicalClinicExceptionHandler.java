package com.arturoller.medicalclinic.exception;

import com.arturoller.medicalclinic.dto.ErrorMessageDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class MedicalClinicExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(MedicalClinicException.class)
    public ResponseEntity<ErrorMessageDto> handleMedicalClinicException(MedicalClinicException e) {
        log.debug("Handling MedicalClinicException", e);
        return error(e.getStatus(), e.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorMessageDto> handleOptimisticLock(ObjectOptimisticLockingFailureException e) {
        log.warn("Optimistic lock conflict", e);
        return error(HttpStatus.CONFLICT, "Dane zostały w międzyczasie zmienione przez inne żądanie, spróbuj ponownie");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorMessageDto> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("Data integrity violation", e);
        return error(HttpStatus.CONFLICT, "Żądanie jest sprzeczne z istniejącymi danymi");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorMessageDto> handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Nieoczekiwany błąd serwera");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + fieldErrorMessage(error))
                .collect(Collectors.joining(", "));
        log.debug("Method argument not valid: {}", message);
        return handleExceptionInternal(e, ProblemDetail.forStatusAndDetail(status, message),
                headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception e, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        String message = resolveMessage(e, body);
        log.debug("Exception internal: {}", message);
        ErrorMessageDto dto = new ErrorMessageDto(message, HttpStatus.valueOf(statusCode.value()));
        return super.handleExceptionInternal(e, dto, headers, statusCode, request);
    }

    // Błąd konwersji (np. "abc" w polu daty) ma domyślnie angielski komunikat Springa – podmieniamy go
    private static String fieldErrorMessage(FieldError error) {
        return error.isBindingFailure() ? "niepoprawny format wartości" : error.getDefaultMessage();
    }

    // Spring zwraca własne, angielskie opisy błędów – zastępujemy je polskimi
    private static String resolveMessage(Exception e, Object body) {
        if (e instanceof MethodArgumentNotValidException && body instanceof ProblemDetail pd && pd.getDetail() != null) {
            return pd.getDetail();
        }
        return switch (e) {
            case HttpMessageNotReadableException ignored ->
                    "Nie można odczytać treści żądania – sprawdź poprawność JSON-a i formaty pól";
            case MethodArgumentTypeMismatchException ex ->
                    "Niepoprawna wartość parametru " + ex.getName() + ": " + ex.getValue();
            case TypeMismatchException ex ->
                    "Niepoprawna wartość pola " + ex.getPropertyName() + ": " + ex.getValue();
            case MissingServletRequestParameterException ex ->
                    "Brak wymaganego parametru: " + ex.getParameterName();
            case HttpRequestMethodNotSupportedException ex ->
                    "Metoda " + ex.getMethod() + " nie jest obsługiwana dla tego adresu";
            case HttpMediaTypeNotSupportedException ex ->
                    "Nieobsługiwany typ treści: " + ex.getContentType() + " – użyj application/json";
            case HttpMediaTypeNotAcceptableException ignored ->
                    "Nie można zwrócić odpowiedzi w żądanym formacie – użyj application/json";
            case NoResourceFoundException ex ->
                    "Nie znaleziono zasobu: " + ex.getResourcePath();
            default -> "Nie udało się przetworzyć żądania";
        };
    }

    private static ResponseEntity<ErrorMessageDto> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorMessageDto(message, status));
    }
}
