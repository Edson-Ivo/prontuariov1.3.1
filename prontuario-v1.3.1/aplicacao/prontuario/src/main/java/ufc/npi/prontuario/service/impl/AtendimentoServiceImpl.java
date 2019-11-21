package ufc.npi.prontuario.service.impl;

import static ufc.npi.prontuario.util.ExceptionSuccessConstants.ERRO_ADICIONAR_ATENDIMENTO;
import static ufc.npi.prontuario.util.ExceptionSuccessConstants.ERRO_EDITAR_ATENDIMENTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ufc.npi.prontuario.exception.ProntuarioException;
import ufc.npi.prontuario.model.Aluno;
import ufc.npi.prontuario.model.Atendimento;
import ufc.npi.prontuario.model.Atendimento.Status;
import ufc.npi.prontuario.model.Avaliacao;
import ufc.npi.prontuario.model.AvaliacaoAtendimento;
import ufc.npi.prontuario.model.ItemAvaliacao;
import ufc.npi.prontuario.model.ItemAvaliacaoAtendimento;
import ufc.npi.prontuario.model.Paciente;
import ufc.npi.prontuario.model.Papel;
import ufc.npi.prontuario.model.Servidor;
import ufc.npi.prontuario.repository.AlunoRepository;
import ufc.npi.prontuario.repository.AtendimentoRepository;
import ufc.npi.prontuario.repository.AvaliacaoAtendimentoRepository;
import ufc.npi.prontuario.repository.AvaliacaoRepository;
import ufc.npi.prontuario.repository.ItemAvaliacaoAtendimentoRepository;
import ufc.npi.prontuario.repository.ItemAvaliacaoRepository;
import ufc.npi.prontuario.repository.ServidorRepository;
import ufc.npi.prontuario.service.AtendimentoService;

@Service
public class AtendimentoServiceImpl implements AtendimentoService {

	@Autowired
	private AtendimentoRepository atendimentoRepository;

	@Autowired
	private AlunoRepository alunoRepository;

	@Autowired
	private ServidorRepository servidorRepository;

	@Autowired
	private AvaliacaoAtendimentoRepository avaliacaoAtendimentoRepository;

	@Autowired
	private ItemAvaliacaoRepository itemAvaliacaoRepository;

	@Autowired
	private AvaliacaoRepository avaliacaoRepository;

	@Autowired
	private ItemAvaliacaoAtendimentoRepository itemAvaliacaoAtendimentoRepository;
	
	@Override
	public Atendimento buscarPorId(Integer id) {
		return atendimentoRepository.findOne(id);
	}

	@Override
	public void salvar(Atendimento atendimento) throws ProntuarioException {
		List<Atendimento> atendimentos = atendimentoRepository.findAllByResponsavelOrAjudanteExist(
				atendimento.getResponsavel(), atendimento.getAjudante(), atendimento.getPaciente(),
				Status.EM_ANDAMENTO);

		if (atendimentos.size() >= 1) {
			throw new ProntuarioException(ERRO_ADICIONAR_ATENDIMENTO);
		}

		atendimento.setStatus(Status.EM_ANDAMENTO);
		atendimentoRepository.save(atendimento);
	}

	@Override
	public void atualizar(Atendimento atendimento) throws ProntuarioException {
		if (atendimentoRepository.exists(atendimento.getId())) {
			atendimentoRepository.save(atendimento);
		} else {
			throw new ProntuarioException(ERRO_EDITAR_ATENDIMENTO);
		}
	}

	@Override
	public List<Atendimento> buscarTudoPorAluno(Aluno aluno) {
		return atendimentoRepository.findAllByResponsavelOrAjudanteOrderByDataDesc(aluno, aluno);

	}

	@Override
	public void finalizarAtendimento(Atendimento atendimento) {
		atendimento.setStatus(Status.REALIZADO);
		atendimentoRepository.save(atendimento);
	}

	@Override
	public void validarAtendimento(Atendimento atendimento) {
		atendimento.setStatus(Status.VALIDADO);
		atendimentoRepository.save(atendimento);
	}

	@Override
	public List<Atendimento> buscarAtendimentosNaoFinalizadosPorProfessor(Servidor servidor) {
		List<Status> status = new ArrayList<Status>();
		status.add(Status.VALIDADO);
		return atendimentoRepository.findAllByProfessorAndStatusNotIn(servidor, status);
	}

	@Override
	public Boolean existeAtendimentoAbertoAlunoPaciente(Aluno aluno, Paciente paciente) {
		List<Atendimento> atendimentos = atendimentoRepository.findAllByResponsavelOrAjudanteExist(aluno, paciente,
				Status.EM_ANDAMENTO);
		return !atendimentos.isEmpty();
	}

	@Override
	public void remover(Atendimento atendimento) {
		atendimentoRepository.delete(atendimento);
	}

	@Override
	public Atendimento ultimoAtendimentoAbertoAlunoPaciente(Aluno aluno, Paciente paciente) {
		List<Atendimento> atendimentos = atendimentoRepository.findAllByResponsavelOrAjudanteExist(aluno, paciente,
				Status.EM_ANDAMENTO);
		if (atendimentos.isEmpty()) {
			return null;
		} else {
			return atendimentos.get(0);
		}
	}

	@Override
	public Atendimento adicionarAvaliacaoAtendimento(Atendimento atendimento) {
		if (Objects.isNull(atendimento.getAvaliacao())) {
			AvaliacaoAtendimento avaliacaoAtendimento = new AvaliacaoAtendimento();
			Avaliacao avaliacao = avaliacaoRepository.findAvaliacaoAtiva();
			for (ItemAvaliacao item : avaliacao.getItens()) {
				avaliacaoAtendimento.addItem(new ItemAvaliacaoAtendimento(item, avaliacaoAtendimento));
			}
			avaliacaoAtendimento.setAvaliacao(avaliacao);
			avaliacaoAtendimentoRepository.saveAndFlush(avaliacaoAtendimento);
			atendimento.setAvaliacao(avaliacaoAtendimento);
			atendimentoRepository.saveAndFlush(atendimento);
		}
		return atendimento;
	}

	@Override
	public Atendimento adicionarItemAvaliacaoAtendimento(Integer item, Atendimento atendimento, String nota,
			Integer avaliacao) {
		Atendimento old = atendimentoRepository.findOne(atendimento.getId());
		ItemAvaliacao itemOld = itemAvaliacaoRepository.findOne(item);
		Avaliacao avaliacaoOld = avaliacaoRepository.findOne(avaliacao);

		old.getAvaliacao().setAvaliacao(avaliacaoOld);

		ItemAvaliacaoAtendimento avaliacaoAtendimento = new ItemAvaliacaoAtendimento(Double.valueOf(nota), itemOld);
		old.getAvaliacao().addItem(avaliacaoAtendimento);
		old.getAvaliacao().setData(new Date());
		atendimentoRepository.save(old);

		return old;
	}

	@Override
	public Atendimento adicionarObservacao(Atendimento atendimento, String observacao) {
		Atendimento old = atendimentoRepository.findOne(atendimento.getId());
		if (observacao != null && observacao.replace(" ", "").length() > 0) {
			old.getAvaliacao().setObservacao(observacao);
			old.getAvaliacao().setData(new Date());
		}
		atendimentoRepository.save(old);

		return old;
	}

	public List<Atendimento> buscarAtendimentoPorPaciente(Paciente paciente) {
		List<Atendimento> atendimentos = new ArrayList<>();
		atendimentos.addAll(atendimentoRepository.findAllByPaciente(paciente));
		return atendimentos;
	}

	@Override
	public Atendimento reavaliarItem(Atendimento atendimento, String nota, Integer item) {

		Atendimento old = atendimentoRepository.findOne(atendimento.getId());
		ItemAvaliacaoAtendimento itemOld = itemAvaliacaoAtendimentoRepository.findOne(item);

		itemOld.setNota(Double.valueOf(nota));
		old.getAvaliacao().setData(new Date());
		itemAvaliacaoAtendimentoRepository.save(itemOld);

		return old;
	}

	@Override
	public List<Atendimento> buscarAtendimentosPorUsuario(Integer idUsuario, Paciente paciente) {
		Aluno aluno = alunoRepository.findOne(idUsuario);
		Servidor servidor = servidorRepository.findOne(idUsuario);

		List<Atendimento> atendimentos = new ArrayList<>();
		if (aluno != null) {
			atendimentos = atendimentoRepository.findAllByResponsavelAndPacienteOrAjudanteAndPaciente(aluno, paciente,
					aluno, paciente);
		} else if (servidor != null) {
			atendimentos = atendimentoRepository.findAllByProfessorAndPaciente(servidor, paciente);
		}
		if (servidor != null && servidor.getPapeis().contains(Papel.ADMINISTRACAO)) {
			atendimentos = this.buscarAtendimentoPorPaciente(paciente);
		}
		if (atendimentos.isEmpty()) {
			atendimentos = atendimentoRepository.findAllByStatusAndPaciente(Status.VALIDADO, paciente);

		} else {

			List<Integer> idAtendimentos = new ArrayList<>();
			for (Atendimento atendimento : atendimentos) {
				idAtendimentos.add(atendimento.getId());
			}

			atendimentos.addAll(atendimentoRepository.findAllByStatusAndPacienteAndIdIsNotIn(Status.VALIDADO, paciente,
					idAtendimentos));
		}
		Collections.sort(atendimentos);

		return atendimentos;
	}

}
