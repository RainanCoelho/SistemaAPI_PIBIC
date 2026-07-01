package com.SistemaApiCrud.SistemaCrud.DTO;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequestDTO {

    private String model;
    private List<MessageDTO> messages;
}
