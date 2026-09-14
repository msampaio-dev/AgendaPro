package com.agendapro.auth.service;

import java.time.Instant;

public record TokenGerado(String valor, Instant expiraEm) {
}
