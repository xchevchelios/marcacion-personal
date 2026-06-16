package com.epesa.demo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class MarcacionBatchRequest {
    @NotEmpty
    @Valid
    private List<MarcacionEventoDto> marcaciones;
}