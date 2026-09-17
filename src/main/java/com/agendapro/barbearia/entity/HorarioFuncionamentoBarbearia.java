package com.agendapro.barbearia.entity;

import java.time.DayOfWeek;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "horarios_funcionamento_barbearia",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_horarios_funcionamento_intervalo",
				columnNames = {"barbearia_id", "dia_semana", "horario_inicio", "horario_fim"}
		)
)
public class HorarioFuncionamentoBarbearia {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "barbearia_id", nullable = false)
	private Barbearia barbearia;

	@Enumerated(EnumType.STRING)
	@Column(name = "dia_semana", nullable = false, length = 9)
	private DayOfWeek diaSemana;

	@Column(name = "horario_inicio", nullable = false)
	private LocalTime horarioInicio;

	@Column(name = "horario_fim", nullable = false)
	private LocalTime horarioFim;

	@Column(nullable = false)
	private boolean ativo = true;

	protected HorarioFuncionamentoBarbearia() {
	}

	public HorarioFuncionamentoBarbearia(
			Barbearia barbearia,
			DayOfWeek diaSemana,
			LocalTime horarioInicio,
			LocalTime horarioFim
	) {
		this.barbearia = barbearia;
		this.diaSemana = diaSemana;
		this.horarioInicio = horarioInicio;
		this.horarioFim = horarioFim;
	}

	public Long getId() { return id; }
	public Barbearia getBarbearia() { return barbearia; }
	public DayOfWeek getDiaSemana() { return diaSemana; }
	public LocalTime getHorarioInicio() { return horarioInicio; }
	public LocalTime getHorarioFim() { return horarioFim; }
	public boolean isAtivo() { return ativo; }
	public void desativar() { ativo = false; }
}
