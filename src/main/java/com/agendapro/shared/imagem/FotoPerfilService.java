package com.agendapro.shared.imagem;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.agendapro.barbearia.entity.Barbearia;
import com.agendapro.barbearia.service.BarbeariaService;
import com.agendapro.profissional.entity.Profissional;
import com.agendapro.profissional.service.ProfissionalService;

@Service
public class FotoPerfilService {
	private final ArmazenamentoImagemService armazenamento;
	private final BarbeariaService barbeariaService;
	private final ProfissionalService profissionalService;

	public FotoPerfilService(
			ArmazenamentoImagemService armazenamento,
			BarbeariaService barbeariaService,
			ProfissionalService profissionalService
	) {
		this.armazenamento = armazenamento;
		this.barbeariaService = barbeariaService;
		this.profissionalService = profissionalService;
	}

	@Transactional
	public Barbearia atualizarFotoBarbearia(Long id, MultipartFile arquivo) {
		Barbearia barbearia = barbeariaService.buscarPorId(id);
		String fotoAnterior = barbearia.getFotoUrl();
		String novaFoto = armazenamento.armazenar(arquivo, "barbearia-" + id);
		barbearia.atualizarFoto(novaFoto);
		agendarSubstituicao(fotoAnterior, novaFoto);
		return barbearia;
	}

	@Transactional
	public Profissional atualizarFotoProfissional(Long id, MultipartFile arquivo) {
		Profissional profissional = profissionalService.buscarPorId(id);
		String fotoAnterior = profissional.getFotoUrl();
		String novaFoto = armazenamento.armazenar(arquivo, "profissional-" + id);
		profissional.atualizarFoto(novaFoto);
		agendarSubstituicao(fotoAnterior, novaFoto);
		return profissional;
	}

	@Transactional
	public void removerFotoBarbearia(Long id) {
		Barbearia barbearia = barbeariaService.buscarPorId(id);
		String fotoAnterior = barbearia.getFotoUrl();
		barbearia.atualizarFoto(null);
		agendarRemocaoDepoisDoCommit(fotoAnterior);
	}

	@Transactional
	public void removerFotoProfissional(Long id) {
		Profissional profissional = profissionalService.buscarPorId(id);
		String fotoAnterior = profissional.getFotoUrl();
		profissional.atualizarFoto(null);
		agendarRemocaoDepoisDoCommit(fotoAnterior);
	}

	private void agendarSubstituicao(String fotoAnterior, String novaFoto) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			armazenamento.remover(fotoAnterior);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				armazenamento.remover(fotoAnterior);
			}

			@Override
			public void afterCompletion(int status) {
				if (status == STATUS_ROLLED_BACK) armazenamento.remover(novaFoto);
			}
		});
	}

	private void agendarRemocaoDepoisDoCommit(String fotoAnterior) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			armazenamento.remover(fotoAnterior);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				armazenamento.remover(fotoAnterior);
			}
		});
	}
}
