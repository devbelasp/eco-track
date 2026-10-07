package br.com.fiap.eco_track_ms.advice;

import br.com.fiap.eco_track_ms.exception.ColetaNaoEncontradaException;
import br.com.fiap.eco_track_ms.model.CategoriaResiduo;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Todas as respostas de erro seguem o formato {"erro": "mensagem"}.
 * Erros de validação incluem também {"campos": {"campo": "mensagem"}}.
 */
@RestControllerAdvice
public class ApplicationExceptionHandler {

    // Campos inválidos ou vazios (@NotBlank, @NotNull, @Positive, @FutureOrPresent...)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, Object> handleInvalidArgument(MethodArgumentNotValidException error) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError campo : error.getBindingResult().getFieldErrors()) {
            campos.put(campo.getField(), campo.getDefaultMessage());
        }

        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("erro", "Dados inválidos.");
        corpo.put("campos", campos);
        return corpo;
    }

    // JSON malformado, categoria inexistente ou data fora do formato AAAA-MM-DD
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Map<String, String> handleCorpoIlegivel() {
        return erro("Corpo da requisição inválido. Categorias aceitas: "
                + Arrays.toString(CategoriaResiduo.values())
                + ". As datas devem seguir o formato AAAA-MM-DD.");
    }

    // ID da URL que não é um número (ex: /agendamentos/abc)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Map<String, String> handleTipoInvalido(MethodArgumentTypeMismatchException error) {
        return erro("O parâmetro '" + error.getName() + "' possui um valor inválido.");
    }

    // Buscar por um ID que não existe na tabela tb_coletas
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ColetaNaoEncontradaException.class)
    public Map<String, String> handleColetaNaoEncontrada(ColetaNaoEncontradaException error) {
        return erro(error.getMessage());
    }

    // Violação de restrição do banco (ex: texto maior que o permitido)
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Map<String, String> handleIntegrityViolation() {
        return erro("A operação violou uma regra de integridade do banco de dados.");
    }

    private Map<String, String> erro(String mensagem) {
        Map<String, String> corpo = new LinkedHashMap<>();
        corpo.put("erro", mensagem);
        return corpo;
    }
}
