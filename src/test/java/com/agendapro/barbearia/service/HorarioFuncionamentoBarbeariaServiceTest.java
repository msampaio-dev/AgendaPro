package com.agendapro.barbearia.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.agendapro.agendamento.repository.AgendamentoRepository;
import com.agendapro.barbearia.entity.Barbearia;
import com.agendapro.barbearia.entity.HorarioFuncionamentoBarbearia;
import com.agendapro.barbearia.exception.OperacaoBarbeariaConflitanteException;
import com.agendapro.barbearia.repository.HorarioFuncionamentoBarbeariaRepository;
import com.agendapro.disponibilidade.entity.HorarioAtendimento;
import com.agendapro.disponibilidade.repository.HorarioAtendimentoRepository;

@ExtendWith(MockitoExtension.class)
class HorarioFuncionamentoBarbeariaServiceTest {

	@Mock private HorarioFuncionamentoBarbeariaRepository repository;
	@Mock private BarbeariaService barbeariaService;
	@Mock private AgendamentoRepository agendamentoRepository;
	@Mock private HorarioAtendimentoRepository horarioAtendimentoRepository;
	@Mock private Clock clock;
	@InjectMocks private HorarioFuncionamentoBarbeariaService service;

	@Test
	void naoDeveRemoverFuncionamentoQueAindaContemJornadaProfissional() {
		Barbearia barbearia = org.mockito.Mockito.mock(Barbearia.class);
		when(barbearia.getId()).thenReturn(3L);
		HorarioFuncionamentoBarbearia funcionamento = new HorarioFuncionamentoBarbearia(
				barbearia, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0));
		HorarioAtendimento jornada = org.mockito.Mockito.mock(HorarioAtendimento.class);
		when(jornada.getHorarioInicio()).thenReturn(LocalTime.of(10, 0));
		when(jornada.getHorarioFim()).thenReturn(LocalTime.of(17, 0));
		when(repository.findById(5L)).thenReturn(Optional.of(funcionamento));
		when(horarioAtendimentoRepository
				.findAllByProfissionalBarbeariaIdAndDiaSemanaAndAtivoTrue(3L, DayOfWeek.MONDAY))
				.thenReturn(List.of(jornada));

		assertThrows(OperacaoBarbeariaConflitanteException.class, () -> service.desativar(5L));

		verify(agendamentoRepository, never())
				.findAllByBarbeariaIdAndStatusInAndInicioAfterOrderByInicio(
						org.mockito.ArgumentMatchers.anyLong(),
						org.mockito.ArgumentMatchers.anyList(),
						org.mockito.ArgumentMatchers.any());
	}
}
