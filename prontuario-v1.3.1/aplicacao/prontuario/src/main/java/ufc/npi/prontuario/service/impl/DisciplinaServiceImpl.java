package ufc.npi.prontuario.service.impl;

import static ufc.npi.prontuario.util.ExceptionSuccessConstants.ERRO_CAMPOS_OBRIGATORIOS;
import static ufc.npi.prontuario.util.ExceptionSuccessConstants.ERRO_EXCLUIR_DISCIPLINA;
import static ufc.npi.prontuario.util.ExceptionSuccessConstants.ERRO_SALVAR_DISCIPLINA_EXISTENTE;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ufc.npi.prontuario.exception.ProntuarioException;
import ufc.npi.prontuario.model.Disciplina;
import ufc.npi.prontuario.repository.DisciplinaRepository;
import ufc.npi.prontuario.service.DisciplinaService;

@Service
public class DisciplinaServiceImpl implements DisciplinaService {

	@Autowired
	private DisciplinaRepository disciplinaRepository;

	@Override
	public void salvar(Disciplina disciplina) throws ProntuarioException {
		if(disciplina.getNome().trim().isEmpty() || disciplina.getCodigo().trim().isEmpty()) {
			throw new ProntuarioException(ERRO_CAMPOS_OBRIGATORIOS);
		}
		if(disciplinaRepository.findByNome(disciplina.getNome()) != null || disciplinaRepository.findByCodigo(disciplina.getCodigo()) != null) {
			throw new ProntuarioException(ERRO_SALVAR_DISCIPLINA_EXISTENTE);
		}
		disciplinaRepository.save(disciplina);
	}

	@Override
	public List<Disciplina> buscarTudo() {
		return disciplinaRepository.findAllByOrderByNome();
	}

	@Override
	public Disciplina buscarPorId(Integer id) {
		return disciplinaRepository.findOne(id);
	}

	@Override
	public void removerDisciplina(Integer id) throws ProntuarioException {
		
		Disciplina disciplina=disciplinaRepository.findOne(id);
		if(disciplina.getTurmas().isEmpty()){
			disciplinaRepository.delete(id);
		}
		else{
			throw new ProntuarioException(ERRO_EXCLUIR_DISCIPLINA);
		}
	}

	@Override
	public void atualizar(Disciplina disciplina) throws ProntuarioException {
		if(disciplina.getId() == null || disciplina.getNome().trim().isEmpty() || disciplina.getCodigo().trim().isEmpty()) {
			throw new ProntuarioException(ERRO_CAMPOS_OBRIGATORIOS);
		}
		Disciplina discExistente = disciplinaRepository.findByNome(disciplina.getNome());
		if(discExistente != null && !discExistente.getId().equals(disciplina.getId())) {
			throw new ProntuarioException(ERRO_SALVAR_DISCIPLINA_EXISTENTE);
		}
		discExistente = disciplinaRepository.findByCodigo(disciplina.getCodigo());
		if(discExistente != null && !discExistente.getId().equals(disciplina.getId())) {
			throw new ProntuarioException(ERRO_SALVAR_DISCIPLINA_EXISTENTE);
		}
		disciplinaRepository.save(disciplina);
	}
}
