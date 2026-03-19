package com.example.Backend_J2EE.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryDTO {

    private Integer id;
    private String name;
    private String description;
}
