package com.SistemaApiCrud.SistemaCrud.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequestDTO {
    //Modelo da IA usada
    private String model;
    //Lista de mensagens da conversa(apenas uma)
    private List<MessageDTO> messages;

}
