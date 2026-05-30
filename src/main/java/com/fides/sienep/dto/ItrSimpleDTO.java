package com.fides.sienep.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ItrSimpleDTO {

    private Integer idItr;

    private String nomItr;
}

/* Como CarreraResponse tendrá una lista de ITRs, conviene crear un DTO pequeño. */