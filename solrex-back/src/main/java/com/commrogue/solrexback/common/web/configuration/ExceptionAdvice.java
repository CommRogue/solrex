/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.common.web.configuration;

import com.commrogue.solrexback.common.exceptions.UnknownEnvironmentException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ExceptionAdvice {
    @ExceptionHandler(UnknownEnvironmentException.class)
    public ResponseEntity<String> handleCustomDeserializationException(UnknownEnvironmentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
