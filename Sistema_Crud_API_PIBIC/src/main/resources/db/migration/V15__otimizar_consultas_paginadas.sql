CREATE INDEX idx_caso_professor_data
    ON casos_clinicos (fk_id_professor, data_criacao DESC);

CREATE INDEX idx_caso_status_data
    ON casos_clinicos (status, data_criacao DESC);

CREATE INDEX idx_pergunta_caso_id
    ON pergunta (fk_id_caso, id);

CREATE INDEX idx_paciente_caso_id
    ON paciente (fk_id_caso, id_paciente);
