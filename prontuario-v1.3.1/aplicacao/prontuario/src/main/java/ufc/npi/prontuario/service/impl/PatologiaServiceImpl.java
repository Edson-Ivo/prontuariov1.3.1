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
import ufc.npi.prontuario.model.Usuario;
import ufc.npi.prontuario.repository.AlunoRepository;
import ufc.npi.prontuario.repository.AtendimentoRepository;
import ufc.npi.prontuario.repository.OdontogramaRepository;
import ufc.npi.prontuario.repository.PatologiaRepository;
import ufc.npi.prontuario.repository.ServidorRepository;
import ufc.npi.prontuario.service.PatologiaService;
import ufc.npi.prontuario.service.TipoPatologiaService;

@Service
public class PatologiaServiceImpl implements PatologiaService {

	@Autowired
	private OdontogramaRepository odontogramaPatologiaRepository;

	@Autowired
	private PatologiaRepository patologiaRepository;
	
	@Autowired
	private TipoPatologiaService tipoPatologiaService;

	@Autowired
	private AtendimentoRepository atendimentoRepository;

	@Autowired
	private AlunoRepository alunoRepository;

	@Autowired
	private ServidorRepository servidorRepository;

	public List<Patologia> salvar(String faceDente, List<Integer> idPatologias, String localString,
			Integer idOdontograma, String descricao, Aluno aluno) throws ProntuarioException {
		Odontograma odontograma = odontogramaPatologiaRepository.findOne(idOdontograma);

		List<Atendimento> atendimentos = atendimentoRepository.findAllByResponsavelOrAjudanteExist(aluno,
				odontograma.getPaciente(), Status.EM_ANDAMENTO);

		if (atendimentos.size() != 1) {
			throw new ProntuarioException(NENHUM_ATENDIMENTO_ABERTO_EXCEPTION);
		}

		FaceDente face = null;
		Dente dente = null;
		Local local = Local.valueOf(localString);
		
		if (local == Local.FACE) {
			face = FaceDente.valueOf(faceDente.substring(3));
			dente = Dente.valueOf("D" + faceDente.substring(0, 2));
		}

		else if (local == Local.DENTE) {
			dente = Dente.valueOf("D" + faceDente);
		}

		Patologia patologia = new Patologia(dente, face, local, descricao, new Date(), odontograma, atendimentos.get(0));
		List<Patologia> patologias = setarTiposPatologias(idPatologias, patologia);
		salvarPatologias(patologias);
		
		return patologias;
	}

	private List<Patologia> setarTiposPatologias(List<Integer> idPatologias, Patologia patologia) {
		List<Patologia> patologias = new ArrayList<Patologia>();
		List<TipoPatologia> tipos = tipoPatologiaService.buscarPorIds(idPatologias);
		
		for (TipoPatologia tipo : tipos) {
			patologia.setTipo(tipo);
			patologias.add(patologia);
		}
		
		return patologias;
	}

	private void salvarPatologias(List<Patologia> patologias) {
		
		for (Patologia patologia : patologias) {			
			patologiaRepository.save(patologia);
		}
	}

	@Override
	public void tratar(Patologia patologia) {
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
