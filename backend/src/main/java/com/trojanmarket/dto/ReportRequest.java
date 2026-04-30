package com.trojanmarket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequest {

    /** "POSTING" or "USER" — the kind of entity being reported. */
    @NotBlank
    private String type;

    @NotNull
    private Integer targetID;

    @Size(max = 1000)
    private String reason;
}
