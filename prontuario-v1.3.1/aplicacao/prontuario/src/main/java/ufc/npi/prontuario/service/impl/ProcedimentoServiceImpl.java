package ufc.npi.prontuario.service.impl;

import static ufc.npi.prontuario.util.ExceptionSuccessConstants.NENHUM_ATENDIMENTO_ABERTO_EXCEPTION;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ufc.npi.prontuario.exception.ProntuarioException;
import ufc.npi.prontuario.model.Aluno;
import ufc.npi.prontuario.model.Atendimento;
import ufc.npi.prontuario.model.Atendimento.Status;
import ufc.npi.prontuario.model.Dente;
import ufc.npi.prontuario.model.FaceDente;
import ufc.npi.prontuario.model.Local;
import ufc.npi.prontuario.model.Odontograma;
import ufc.npi.prontuario.model.Patologia;
import ufc.npi.prontuario.model.Procedimento;
import ufc.npi.prontuario.model.Servidor;
import ufc.npi.prontuario.model.TipoProcedimento;
import ufc.npi.prontuario.model.Tratamento;
import ufc.npi.prontuario.model.Usuario;
import ufc.npi.prontuario.repository.AlunoRepository;
import ufc.npi.prontuario.repository.AtendimentoRepository;
import ufc.npi.prontuario.repository.OdontogramaRepository;
import ufc.npi.prontuario.repository.PatologiaRepository;
import ufc.npi.prontuario.repository.ProcedimentoRepository;
import ufc.npi.prontuario.repository.ServidorRepository;
import ufc.npi.prontuario.repository.TipoProcedimentoRepository;
import ufc.npi.prontuario.repository.UsuarioRepository;
import ufc.npi.prontuario.service.ProcedimentoService;

@Service
public class ProcedimentoServiceImpl implements ProcedimentoService {

	@Autowired
	private OdontogramaRepository odontogramaPatologiaRepository;

	@Autowired
	private ProcedimentoRepository procedimentoRepository;

	@Autowired
	private TipoProcedimentoRepository tipoProcedimentoRepository;

	@Autowired
	private AtendimentoRepository atendimentoRepository;

	@Autowired
	private AlunoRepository alunoRepository;

	@Autowired
	private ServidorRepository servidorRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private PatologiaRepository patologiaRepository;



	public List<Procedimento> salvar(String faceDente, List<Integer> idProcedimentos, String localString,
			Integer idOdontograma, String descricao, Aluno aluno, Boolean preExistente, List<Integer> patologias,
			Date data) throws ProntuarioException {
		Odontograma odontograma = odontogramaPatologiaRepository.findOne(idOdontograma);

		// Verificação de restrições
		List<Atendimento> atendimentos = buscarAtendimentos(aluno, odontograma);

		verificarSeAtendimentoEstaAberto(atendimentos);
		// Fim da verificação de restrições

		List<Procedimento> procedimentos = new ArrayList<>();
		List<TipoProcedimento> tipoProcedimentos = getListTipoProcedimento(idProcedimentos);

		Procedimento procedimento = configurarProcedimentos(localString, faceDente, descricao, preExistente, atendimentos, odontograma);
		
		procedimentos = salvarProcedimentos(tipoProcedimentos, procedimento);
		
		salvarPatologias(patologias, data, descricao, aluno);

		return procedimentos;
	}

	private List<Atendimento> buscarAtendimentos(Aluno aluno, Odontograma odontograma) {
		return atendimentoRepository.findAllByResponsavelOrAjudanteExist(aluno,
				odontograma.getPaciente(), Status.EM_ANDAMENTO);
	}

	private void verificarSeAtendimentoEstaAberto(List<Atendimento> atendimentos) throws ProntuarioException  {
		if (atendimentos.size() != 1) {
			throw new ProntuarioException(NENHUM_ATENDIMENTO_ABERTO_EXCEPTION);
		}
	}

	private Procedimento configurarProcedimentos(String localString, String faceDente, String descricao, Boolean preExistente,
												 List<Atendimento> atendimentos, Odontograma odontograma) {
		Local local = Local.valueOf(localString);

		FaceDente face = null;
		Dente dente = null;

		if (local == Local.FACE) {
			face = FaceDente.valueOf(faceDente.substring(3));
			dente = Dente.valueOf("D" + faceDente.substring(0, 2));

		} else if (local == Local.DENTE) {
			dente = Dente.valueOf("D" + faceDente);
		}

		return new Procedimento(dente, face, local, null,
				descricao, atendimentos.get(0), odontograma, preExistente);
	}

	private void salvarPatologias(List<Integer> patologias, Date data, String descricao, Aluno aluno) {

		if (patologias != null && !patologias.isEmpty()) {

			Tratamento tratamento = new Tratamento();
			tratamento.setData(data);
			tratamento.setDescricao(descricao);
			tratamento.setResponsavel(aluno);

			for (Integer p : patologias) {
				Patologia patologia = patologiaRepository.findOne(p);
				tratamento.setPatologia(patologia);
				patologia.setTratamento(tratamento);
				patologiaRepository.saveAndFlush(patologia);
			}
		}
	}

	private List<Procedimento> salvarProcedimentos(List<TipoProcedimento> tipoProcedimentos, Procedimento procedimento) {
		
		List<Procedimento> procedimentos = new ArrayList<>();
		
		for (TipoProcedimento tipo : tipoProcedimentos) {
			procedimento.setTipoProcedimento(tipo);
			procedimentos.add(procedimento);
			procedimentoRepository.save(procedimento);
		}
		
		return procedimentos;
	}
	
	private List<TipoProcedimento> getListTipoProcedimento(List<Integer> idProcedimentos){
		List<TipoProcedimento> tipoProcedimentos = new ArrayList<>();
		
		for (Integer idTipo : idProcedimentos) {
			TipoProcedimento tipo = tipoProcedimentoRepository.findOne(idTipo);
			tipoProcedimentos.add(tipo);
		}
		
		return tipoProcedimentos;
	}

	@Override
	public List<Procedimento> buscarProcedimentosOdontograma(Odontograma odontograma, Integer idUsuarioLogado) {
		Aluno aluno = alunoRepository.findOne(idUsuarioLogado);
		Servidor servidor = servidorRepository.findOne(idUsuarioLogado);

		List<Procedimento> procedimentos = new ArrayList<>();

		// Adicionando os procedimentos em que o usuário logado é o responsável,
		// ajudante ou professor
		if (aluno != null) {
			procedimentos = procedimentoRepository
					.findAllByOdontogramaAndAtendimentoResponsavelAndPreExistenteIsFalseOrOdontogramaAndAtendimentoAjudanteAndPreExistenteIsFalse(
							odontograma, aluno, odontograma, aluno);
		} else if (servidor != null) {
			procedimentos = procedimentoRepository
					.findAllByOdontogramaAndAtendimentoProfessorAndPreExistenteIsFalse(odontograma, servidor);
		}

		// Se o usuário logado não fez parte do atendimento a lista está vazia,
		// e são carregadas apenas os procedimentos validados
		if (procedimentos.isEmpty()) {
			procedimentos = procedimentoRepository
					.findAllByOdontogramaAndAtendimentoStatusAndPreExistenteIsFalse(odontograma, Status.VALIDADO);

			// Se o usuário logado faz parte do atendimento então a lista não
			// está vazia e são adicionadas os procedimentos adicionados
			// por outras pessoas que estão validados
		} else {

			List<Integer> idProcedimentos = new ArrayList<>();
			for (Procedimento procedimento : procedimentos) {
				idProcedimentos.add(procedimento.getId());
			}

			procedimentos.addAll(
					procedimentoRepository.findAllByOdontogramaAndIdIsNotInAndAtendimentoStatusAndPreExistenteIsFalse(
							odontograma, idProcedimentos, Status.VALIDADO));
		}

		Collections.sort(procedimentos);
		return procedimentos;
	}

	/*
	 * @Override public List<Procedimento> buscarProcedimentosTabela(Odontograma
	 * odontograma, Integer idUsuarioLogado) { return
	 * this.buscarProcedimentosOdontograma(odontograma, idUsuarioLogado); }
	 */

	@Override
	public List<Procedimento> buscarProcedimentosExistentesOdontograma(Odontograma odontograma,
			Integer idUsuarioLogado) {
		Usuario usuario = usuarioRepository.findOne(idUsuarioLogado);

		List<Procedimento> procedimentos = new ArrayList<>();

		// Adicionando os procedimentos em que o usuário logado é o responsável,
		// ajudante ou professor
		if (usuario instanceof Aluno) {
			procedimentos = procedimentoRepository
					.findAllByOdontogramaAndAtendimentoResponsavelAndPreExistenteIsTrueOrOdontogramaAndAtendimentoAjudanteAndPreExistenteIsTrue(
							odontograma, (Aluno) usuario, odontograma, (Aluno) usuario);
		} else if (usuario instanceof Servidor) {
			procedimentos = procedimentoRepository
					.findAllByOdontogramaAndAtendimentoProfessorAndPreExistenteIsTrue(odontograma, (Servidor) usuario);
		}

		// Se o usuário logado não fez parte do atendimento a lista está vazia,
		// e são carregadas apenas os procedimentos validados
		if (procedimentos.isEmpty()) {
			procedimentos = procedimentoRepository
					.findAllByOdontogramaAndAtendimentoStatusAndPreExistenteIsTrue(odontograma, Status.VALIDADO);

			// Se o usuário logado faz parte do atendimento então a lista não
			// está vazia e são adicionadas os procedimentos adicionados
			// por outras pessoas que estão validados
		} else {

			List<Integer> idProcedimentos = new ArrayList<>();
			for (Procedimento procedimento : procedimentos) {
				idProcedimentos.add(procedimento.getId());
			}

			procedimentos.addAll(
					procedimentoRepository.findAllByOdontogramaAndIdIsNotInAndAtendimentoStatusAndPreExistenteIsTrue(
							odontograma, idProcedimentos, Status.VALIDADO));
		}

		procedimentos.sort((p1, p2) -> p1.getAtendimento().getData().compareTo(p1.getAtendimento().getData()));
		return procedimentos;
	}

	@Override
	public void deletar(Procedimento procedimento) {
		procedimento.getOdontograma().getProcedimentos().remove(procedimento);
		procedimento.getAtendimento().getProcedimentos().remove(procedimento);
		procedimentoRepository.delete(procedimento);
	}

}
