package ufc.npi.prontuario.service.impl;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ufc.npi.prontuario.exception.ProntuarioException;
import ufc.npi.prontuario.model.Disciplina;
import ufc.npi.prontuario.model.Paciente;
import ufc.npi.prontuario.model.PlanoTratamento;
import ufc.npi.prontuario.model.Servidor;
import ufc.npi.prontuario.model.PlanoTratamento.Status;
import ufc.npi.prontuario.repository.PlanoTratamentoRepository;
import ufc.npi.prontuario.service.PlanoTratamentoService;
import static ufc.npi.prontuario.util.ExceptionSuccessConstants.ERRO_ADD_PLANO_TRATAMENTO;
import static ufc.npi.prontuario.util.ExceptionSuccessConstants.ERROR_EXCLUIR_PLANO_TRATAMENTO;
import static ufc.npi.prontuario.util.ExceptionSuccessConstants.ERROR_FINALIZAR_PLANO_TRATAMENTO;
import static ufc.npi.prontuario.util.ExceptionSuccessConstants.ERROR_EDITAR_PLANO_TRATAMENTO;

@Service
public class PlanoTratamentoServiceImpl implements PlanoTratamentoService {

	@Autowired
	private PlanoTratamentoRepository planoTratamentoRepository;

	@Override
	public List<PlanoTratamento> buscarPlanoTratamentoPorPaciente(Paciente paciente) {
		return planoTratamentoRepository.findAllByPaciente(paciente);
	}

	@Override
	public void excluirPlanoTratamento(PlanoTratamento planoTratamento) throws ProntuarioException {
		PlanoTratamento old = planoTratamentoRepository.findOne(planoTratamento.getId());
		if (!old.getStatus().equals(Status.EM_ESPERA)) {
			throw new ProntuarioException(ERROR_EXCLUIR_PLANO_TRATAMENTO);
		} else {
			planoTratamentoRepository.delete(old);
		}
	}

	@Override
	public void salvar(PlanoTratamento planoTratamento, Servidor responsavel, Paciente paciente) 
			throws ProntuarioException {
		List<Status> statuses = Arrays.asList(Status.EM_ESPERA, Status.EM_ANDAMENTO);
		Integer temTratamento = planoTratamentoRepository.
				countByPacienteAndClinicaAndStatusIn(planoTratamento.getPaciente(),
						planoTratamento.getClinica(), statuses);
		
		if (temTratamento == 0) {
			planoTratamento.setResponsavel(responsavel);
			planoTratamento.setPaciente(paciente);
			planoTratamentoRepository.save(planoTratamento);
		} else {
			throw new ProntuarioException(ERRO_ADD_PLANO_TRATAMENTO);
		}
	}

	@Override
	public void finalizar(PlanoTratamento planoTratamento) throws ProntuarioException {
		PlanoTratamento old = planoTratamentoRepository.findOne(planoTratamento.getId());
		if (old.getStatus().equals(Status.EM_ESPERA)) {
			old.setStatus(Status.CONCLUIDO);
			planoTratamentoRepository.save(old);
		} else {
			throw new ProntuarioException(ERROR_FINALIZAR_PLANO_TRATAMENTO);
		}

	}

	@Override
	public List<PlanoTratamento> buscarPlanoTratamentoPorClinicaEStatus(Disciplina disciplina, String status) {

		if (!status.equalsIgnoreCase("")) {
			Status s = Status.valueOf(status);
			return planoTratamentoRepository.findByClinicaAndStatus(disciplina, s);
		} else {
			return planoTratamentoRepository.findByClinica(disciplina);
		}

	}

	@Override
	public void editar(PlanoTratamento planoTratamento) throws ProntuarioException {
		PlanoTratamento old = planoTratamentoRepository.findOne(planoTratamento.getId());
		if (old == null || 
				(old.getStatus().equals(Status.CONCLUIDO) || 
						old.getStatus().equals(Status.INTERROMPIDO))) {
			throw new ProntuarioException(ERROR_EDITAR_PLANO_TRATAMENTO);
		} else {
			planoTratamentoRepository.save(planoTratamento);
		}
	}

}
