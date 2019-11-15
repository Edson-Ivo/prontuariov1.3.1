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
import ufc.npi.prontuario.model.Papel;
import ufc.npi.prontuario.model.Patologia;
import ufc.npi.prontuario.model.Servidor;
import ufc.npi.prontuario.model.TipoPatologia;
import ufc.npi.prontuario.model.Tratamento;
import ufc.npi.prontuario.model.Usuario;
import ufc.npi.prontuario.repository.AlunoRepository;
import ufc.npi.prontuario.repository.AtendimentoRepository;
import ufc.npi.prontuario.repository.OdontogramaRepository;
import ufc.npi.prontuario.repository.PatologiaRepository;
import ufc.npi.prontuario.repository.ServidorRepository;
import ufc.npi.prontuario.repository.TipoPatologiaRepository;
import ufc.npi.prontuario.service.PatologiaService;

@Service
public class PatologiaServiceImpl implements PatologiaService {

	@Autowired
	private OdontogramaRepository odontogramaPatologiaRepository;

	@Autowired
	private PatologiaRepository patologiaRepository;

	@Autowired
	private TipoPatologiaRepository tipoPatologiaRepository;

	@Autowired
	private AtendimentoRepository atendimentoRepository;

	@Autowired
	private AlunoRepository alunoRepository;

	@Autowired
	private ServidorRepository servidorRepository;

	public List<Patologia> salvar(String faceDente, List<Integer> idPatologias, String localString,
			Integer idOdontograma, String descricao, Aluno aluno) throws ProntuarioException {
		Odontograma odontograma = odontogramaPatologiaRepository.findOne(idOdontograma);

		// Verificação de restrições
		List<Atendimento> atendimentos = atendimentoRepository.findAllByResponsavelOrAjudanteExist(aluno,
				odontograma.getPaciente(), Status.EM_ANDAMENTO);

		if (atendimentos.size() != 1) {
			throw new ProntuarioException(NENHUM_ATENDIMENTO_ABERTO_EXCEPTION);
		}
		// Fim da verificação de restrições

		List<Patologia> patologias = new ArrayList<Patologia>();

		Local local = Local.valueOf(localString);

		FaceDente face = null;
		Dente dente = null;

		if (local == Local.FACE) {
			face = FaceDente.valueOf(faceDente.substring(3));
			dente = Dente.valueOf("D" + faceDente.substring(0, 2));

			patologias = salvarPatologias(idPatologias, face, dente, local, odontograma, descricao, patologias,
					atendimentos.get(0));
		}

		else if (local == Local.DENTE) {
			face = null;
			dente = Dente.valueOf("D" + faceDente);
			patologias = salvarPatologias(idPatologias, face, dente, local, odontograma, descricao, patologias,
					atendimentos.get(0));
		}

		else if (local == Local.GERAL) {
			patologias = salvarPatologias(idPatologias, face, dente, local, odontograma, descricao, patologias,
					atendimentos.get(0));
		}

		return patologias;
	}

	private List<Patologia> salvarPatologias(List<Integer> idPatologias, FaceDente face, Dente dente, Local local,
			Odontograma odontograma, String descricao, List<Patologia> patologias, Atendimento atendimento) {

		for (Integer p : idPatologias) {
			TipoPatologia tipo = tipoPatologiaRepository.findOne(p);

			Patologia patologia = new Patologia();
			patologia.setDente(dente);
			patologia.setFace(face);
			patologia.setLocal(local);
			patologia.setTipo(tipo);
			patologia.setData(new Date());
			patologia.setOdontograma(odontograma);
			patologia.setDescricao(descricao);
			patologia.setAtendimento(atendimento);

			patologiaRepository.save(patologia);

			patologias.add(patologia);
		}

		return patologias;
	}

	@Override
	public void tratar(Patologia patologia, Tratamento tratamento) {
		patologia.setTratamento(tratamento);
		patologiaRepository.saveAndFlush(patologia);
	}

	@Override
	public List<Patologia> buscarPatologiasOdontograma(Odontograma odontograma, Usuario usuario) {
		Aluno aluno = alunoRepository.findOne(usuario.getId());
		Servidor servidor = servidorRepository.findOne(usuario.getId());

		List<Patologia> patologias = new ArrayList<>();

		// Adicionando as patologias que o usuário logado é o responsável,
		// ajudante ou professor
		
		if (aluno != null) {
			patologias = patologiaRepository
					.findAllByOdontogramaAndAtendimentoResponsavelOrOdontogramaAndAtendimentoAjudante(odontograma,
							aluno, odontograma, aluno);
		} else if (servidor != null) {
			patologias = patologiaRepository.findAllByOdontogramaAndAtendimentoProfessor(odontograma, servidor);
		}
		if(usuario.getPapeis().contains(Papel.ADMINISTRACAO)){
			patologias = patologiaRepository.findAllByOdontograma(odontograma);
		}

		// Se o usuário logado não fez parte do atendimento a lista está vazia,
		// e são carregadas apenas as patologias validadas
		if (patologias.isEmpty()) {
			patologias = patologiaRepository.findAllByOdontogramaAndAtendimentoStatus(odontograma, Status.VALIDADO);

			// Se o usuário logado faz parte do atendimento então a lista não
			// está vazia e são adicionadas as patologias adicionadas
			// por outras pessoas que estão validadas
		} else {

			List<Integer> idPatologias = new ArrayList<>();
			for (Patologia patologia : patologias) {
				idPatologias.add(patologia.getId());
			}

			patologias.addAll(patologiaRepository.findAllByOdontogramaAndIdIsNotInAndAtendimentoStatus(odontograma,
					idPatologias, Status.VALIDADO));
		}
		
		Collections.sort(patologias);

		return patologias;
	}

	@Override
	public List<Patologia> buscarPatologiasTratadas(Odontograma odontograma) {
		return patologiaRepository.findByOdontogramaAndTratamentoIsNotNull(odontograma);
	}

	@Override
	public List<Patologia> buscarPatologiasDentePaciente(Odontograma odontograma, String faceDente, String dente, Aluno aluno) {
		List<Patologia> patologias = new ArrayList<>();

		Dente d = null;
		FaceDente f = null;

		if (faceDente != null) {
			d = Dente.valueOf("D" + faceDente.substring(0, 2));
			f = FaceDente.valueOf(faceDente.substring(3, 4));
		} else if (dente != null) {
			d = Dente.valueOf("D" + dente);
		}

		for (Patologia p : odontograma.getPatologias()) {
			if ((p.getAtendimento().isValidado() || p.getAtendimento().getResponsavel().equals(aluno))
					&& p.getTratamento() == null && (p.getLocal().equals(Local.GERAL) 
					|| (p.getDente() != null && p.getDente().equals(d)
						&& (p.getFace() == null || p.getFace().equals(f))))) {
				patologias.add(p);
			}
		}

		return patologias;
	}

	public void deletar(Patologia patologia) {
		patologia.getOdontograma().getPatologias().remove(patologia);
		patologia.getAtendimento().getPatologias().remove(patologia);
		patologiaRepository.delete(patologia);
	}
}
