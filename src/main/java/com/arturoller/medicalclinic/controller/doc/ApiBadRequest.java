package com.arturoller.medicalclinic.controller.doc;

import com.arturoller.medicalclinic.dto.ErrorMessageDto;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ApiResponse(responseCode = "400",
        content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorMessageDto.class),
                examples = @ExampleObject(value = """
                        {"message": "firstName: nie może być puste", "status": "BAD_REQUEST"}""")))
public @interface ApiBadRequest {
    @AliasFor(annotation = ApiResponse.class, attribute = "description")
    String value() default "Niepoprawne dane wejściowe";
}
