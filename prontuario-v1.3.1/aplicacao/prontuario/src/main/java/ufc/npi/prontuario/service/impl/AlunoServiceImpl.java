package ufc.npi.prontuario.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ufc.npi.prontuario.exception.ProntuarioException;
import ufc.npi.prontuario.model.Aluno;
import ufc.npi.prontuario.model.Papel;
import ufc.npi.prontuario.repository.AlunoRepository;
import ufc.npi.prontuario.service.AlunoService;

import static ufc.npi.prontuario.util.ExceptionSuccessConstants.*;


@Service
public class AlunoServiceImpl implements AlunoService {

	@Autowired
	private AlunoRepository alunoRepository;

	public Aluno buscarPorId(Integer id) {
		return alunoRepository.findOne(id);
	}

	@Override
	public void salvar(Aluno aluno) throws ProntuarioException {
		if(aluno.getNome().trim().isEmpty() || aluno.getEmail().trim().isEmpty() || aluno.getMatricula().trim().isEmpty()
				|| aluno.getAnoIngresso() == null){
			throw new ProntuarioException(ERRO_CAMPOS_OBRIGATORIOS);
		}
		if(buscarPorMatricula(aluno.getMatricula()) != null){
			throw new ProntuarioException(ERRO_ADICIONAR_ALUNO);
		}
		aluno.addPapel(Papel.ESTUDANTE);
		aluno.setSenha(aluno.getMatricula());
		aluno.encodePassword();
		alunoRepository.save(aluno);
	}
	
	public void atualizar(Aluno aluno) throws ProntuarioException {
		if(aluno.getNome().trim().isEmpty() || aluno.getEmail().trim().isEmpty() || aluno.getMatricula().trim().isEmpty()
				|| aluno.getAnoIngresso() == null){
			throw new ProntuarioException(ERRO_CAMPOS_OBRIGATORIOS);
		}
			
		Aluno alunoExistente = buscarPorMatricula(aluno.getMatricula());
		if(alunoExistente != null && !alunoExistente.getId().equals(aluno.getId())) {
			throw new ProntuarioException(ERRO_ATUALIZAR_ALUNO);
		}
		aluno.setSenha(alunoRepository.findOne(aluno.getId()).getSenha());
		alunoRepository.save(aluno);
	}
	
	@Override
	public void removerAluno(Integer id) throws ProntuarioException {
		Aluno aluno=alunoRepository.findOne(id);
		if(aluno.getAlunoTurmas().isEmpty() && aluno.getAtendimentos().isEmpty()){
			alunoRepository.delete(id);
		} else {
			throw new ProntuarioException(ERRO_EXCLUIR_ALUNO);
		}
	}

	@Override
	public List<Aluno> buscarTudo() {
		return alunoRepository.findAll();
	}

	@Override
	public Aluno buscarPorMatricula(String matricula) {
		return alunoRepository.findByMatricula(matricula);
	}

	@Override
	public List<Aluno> buscarAjudantes(Integer idTurma, Aluno responsavel) {
		if(alunoRepository.exists(responsavel.getId())){
			List<Aluno> ajudantes = alunoRepository.findAllByAlunoTurmasTurmaIdEqualsAndAlunoTurmasAtivoIsTrue(idTurma);
			ajudantes.remove(responsavel);
			return ajudantes;
		}
		
		return null;
	}
	
}
