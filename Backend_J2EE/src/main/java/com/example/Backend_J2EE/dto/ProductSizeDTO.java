package com.example.Backend_J2EE.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductSizeDTO {

    private Integer id;
    private Integer sizeId;
    private String sizeName;
    private Integer quantity;
}
