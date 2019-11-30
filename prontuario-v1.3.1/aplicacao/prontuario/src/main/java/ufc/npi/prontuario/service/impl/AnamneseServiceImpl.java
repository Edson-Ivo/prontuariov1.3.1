package ufc.npi.prontuario.service.impl;

import static ufc.npi.prontuario.util.ExceptionSuccessConstants.ERROR_EXCLUIR_ANAMNESE;
import static ufc.npi.prontuario.util.ExceptionSuccessConstants.ERROR_NOME_ANAMNESE;
import static ufc.npi.prontuario.util.ExceptionSuccessConstants.ERRO_CAMPOS_OBRIGATORIOS;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ufc.npi.prontuario.exception.ProntuarioException;
import ufc.npi.prontuario.model.Anamnese;
import ufc.npi.prontuario.model.Anamnese.Status;
import ufc.npi.prontuario.model.Pergunta;
import ufc.npi.prontuario.repository.AnamneseRepository;
import ufc.npi.prontuario.repository.PerguntaRepository;
import ufc.npi.prontuario.service.AnamneseService;

@Service
public class AnamneseServiceImpl implements AnamneseService {

	@Autowired
	private AnamneseRepository anamneseRepository;

	@Autowired
	PerguntaRepository perguntaRepository;

	@Override
	public void salvar(Anamnese anamnese) throws ProntuarioException {
		if (anamnese.getNome().trim().isEmpty() || anamnese.getDescricao().trim().isEmpty()) {
			throw new ProntuarioException(ERRO_CAMPOS_OBRIGATORIOS);
		}
		if (anamneseRepository.findByNome(anamnese.getNome()) != null) {
			throw new ProntuarioException(ERROR_NOME_ANAMNESE);
		}
		anamnese.setStatus(Status.EM_ANDAMENTO);
		anamneseRepository.save(anamnese);
	}

	@Override
	public void salvarPergunta(Pergunta pergunta, Integer idAnamnese) {

		Anamnese anamnese = anamneseRepository.findOne(idAnamnese);

		if (anamnese != null && anamnese.getStatus() != Status.FINALIZADA) {
			if (anamnese.getPerguntas().isEmpty()) {
				pergunta.setOrdem(1);
			} else {
				pergunta.setOrdem(anamnese.getPerguntas().get(anamnese.getPerguntas().size() - 1).getOrdem() + 1);
			}
			pergunta.setAnamnese(anamnese);

			anamnese.getPerguntas().add(pergunta);

			anamneseRepository.save(anamnese);
		}
	}

	@Override
	public void excluirPergunta(Pergunta pergunta, Anamnese anamnese) {
		perguntaRepository.delete(pergunta);
		
		removerPerguntaDaAnamnese(anamnese, pergunta);
	}
	
	private void removerPerguntaDaAnamnese(Anamnese anamnese, Pergunta pergunta) {
		if (anamnese.getStatus() != Status.FINALIZADA) {
			List<Pergunta> perguntas = anamnese.getPerguntas();
			
			removerPerguntaDaLista(perguntas, pergunta);
			
			anamneseRepository.saveAndFlush(anamnese);
		}
	}
	
	private void removerPerguntaDaLista(List<Pergunta> perguntas, Pergunta pergunta) {
		int posicaoPergunta = perguntas.indexOf(pergunta);
		
		if (posicaoPergunta > -1) {
			List<Pergunta> perguntasParaReordenar = perguntas.subList(posicaoPergunta + 1, perguntas.size());
			
			for (Pergunta perguntaParaReordenar : perguntasParaReordenar) {
				Integer novaOrdem = perguntaParaReordenar.getOrdem() - 1;
				perguntaParaReordenar.setOrdem(novaOrdem);
			}
			
			perguntas.remove(posicaoPergunta);
		}
	}

	@Override
	public List<Anamnese> buscarTudo() {
		List<Anamnese> anamneses = anamneseRepository.findAll();
		return anamneses;
	}

	@Override
	public Anamnese buscarPorId(Integer id) {
		Anamnese anamnese = anamneseRepository.findOne(id);
		return anamnese;
	}

	@Override
	public List<Anamnese> buscarTodasFinalizadas() {
		return anamneseRepository.findAllByStatus(Status.FINALIZADA);
	}

	@Override
	public void remover(Anamnese anamnese) throws ProntuarioException {
		if (anamnese != null && anamnese.getStatus().equals(Status.FINALIZADA)) {
			throw new ProntuarioException(ERROR_EXCLUIR_ANAMNESE);
		}
		anamneseRepository.delete(anamnese);
	}

	@Override
	public void finalizar(Anamnese anamnese) {
		anamnese.setStatus(Status.FINALIZADA);
		anamneseRepository.save(anamnese);
	}

	@Override
	public Anamnese alterarOrdemAnamnese(Anamnese anamnese, Integer pergunta, Integer novaOrdem) {
		Pergunta old = perguntaRepository.findOne(pergunta);
		Anamnese anamneseOld = anamneseRepository.findOne(anamnese.getId());
		if (anamneseOld.getStatus().equals(Status.EM_ANDAMENTO)) {

			Pergunta a = anamneseOld.getPerguntas().get(novaOrdem - 1);
			int indexPerguntaAntiga = anamneseOld.getPerguntas().indexOf(a);
			int indexPergunta = anamneseOld.getPerguntas().indexOf(old);

			anamneseOld.getPerguntas().remove(old);
			if (novaOrdem > old.getOrdem()) {
				for (int i = indexPergunta; i < indexPerguntaAntiga; i++) {
					anamneseOld.getPerguntas().get(i).setOrdem(anamneseOld.getPerguntas().get(i).getOrdem() - 1);
				}

			} else {
				for (int i = indexPergunta - 1; i >= indexPerguntaAntiga; i--) {
					anamneseOld.getPerguntas().get(i).setOrdem(anamneseOld.getPerguntas().get(i).getOrdem() + 1);
				}
			}
			old.setOrdem(novaOrdem);
			anamneseOld.addPergunta(old);
			Collections.sort(anamneseOld.getPerguntas());
			anamneseRepository.save(anamneseOld);
		}
		return anamneseOld;

	}

}
