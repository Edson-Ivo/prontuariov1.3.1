package ufc.npi.prontuario.service.impl;


import static ufc.npi.prontuario.util.ExceptionSuccessConstants.ERRO_ADICIONAR_SERVIDOR;
import static ufc.npi.prontuario.util.ExceptionSuccessConstants.ERRO_ADICIONAR_SERVIDOR_PAPEL;
import static ufc.npi.prontuario.util.ExceptionSuccessConstants.ERRO_ADICIONAR_SERVIDOR_VAZIO;
import static ufc.npi.prontuario.util.ExceptionSuccessConstants.ERRO_CAMPOS_OBRIGATORIOS;
import static ufc.npi.prontuario.util.ExceptionSuccessConstants.ERRO_EXCLUIR_SERVIDOR;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ufc.npi.prontuario.exception.ProntuarioException;
import ufc.npi.prontuario.model.Servidor;
import ufc.npi.prontuario.model.Usuario;
import ufc.npi.prontuario.repository.AtendimentoRepository;
import ufc.npi.prontuario.repository.ServidorRepository;
import ufc.npi.prontuario.repository.TurmaRepository;
import ufc.npi.prontuario.repository.UsuarioRepository;
import ufc.npi.prontuario.service.ServidorService;

@Service
public class ServidorServiceImpl implements ServidorService {

	@Autowired
	private ServidorRepository servidorRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private AtendimentoRepository atendimentoRepository;

	@Autowired
	private TurmaRepository turmaRepository;

	@Override
	public void salvar(Servidor servidor) throws ProntuarioException {
		if(servidor.getNome().replaceAll(" ", "").length() == 0){
			throw new ProntuarioException(ERRO_ADICIONAR_SERVIDOR_VAZIO);
		}
		
		if (usuarioRepository.findByEmail(servidor.getEmail()) != null || usuarioRepository.findByMatricula(servidor.getMatricula()) != null) {
			throw new ProntuarioException(ERRO_ADICIONAR_SERVIDOR);
		}
				
		if(servidor.getPapeis().size() == 0){
			throw new ProntuarioException(ERRO_ADICIONAR_SERVIDOR_PAPEL);
		}
		
		
		
		servidor.setSenha(servidor.getMatricula());
		servidor.encodePassword();
		servidorRepository.save(servidor);
	}

	public void atualizar(Servidor servidor) throws ProntuarioException {
		if(servidor.getId() == null || servidor.getNome().trim().isEmpty() || servidor.getEmail().trim().isEmpty() || servidor.getMatricula().trim().isEmpty()) {
			throw new ProntuarioException(ERRO_CAMPOS_OBRIGATORIOS);
		}
		Usuario aux = usuarioRepository.findByEmail(servidor.getEmail());
		if(aux != null && !aux.getId().equals(servidor.getId())) {
			throw new ProntuarioException(ERRO_ADICIONAR_SERVIDOR);
		}
		aux = usuarioRepository.findByMatricula(servidor.getMatricula());
		if(aux != null && !aux.getId().equals(servidor.getId())) {
			throw new ProntuarioException(ERRO_ADICIONAR_SERVIDOR);
		}
		
		if(servidor.getPapeis().size() == 0){
			throw new ProntuarioException(ERRO_ADICIONAR_SERVIDOR_PAPEL);
		}
		
		if(servidor.getNome().replaceAll(" ", "").length() == 0){
			throw new ProntuarioException(ERRO_ADICIONAR_SERVIDOR_VAZIO);
		}
		
		

		servidor.setSenha(servidorRepository.findOne(servidor.getId()).getSenha());
		servidorRepository.save(servidor);
	}

	/*@Override
	public List<Servidor> buscarTudo() {
		return servidorRepository.findAllByOrderByNome();
	}*/

	@Override
	public List<Servidor> buscarProfessores() {
		return servidorRepository.findAll();
	}

	@Override
	public Servidor buscarPorId(Integer id) {
		return servidorRepository.findOne(id);
	}

	@Override
	public void removerServidor(Integer id) throws ProntuarioException {
		Servidor professor = servidorRepository.findOne(id);
		if (turmaRepository.findAllByProfessores(professor).isEmpty()
				&& atendimentoRepository.findAllByProfessor(professor).isEmpty()) {
			servidorRepository.delete(id);
		} else {
			throw new ProntuarioException(ERRO_EXCLUIR_SERVIDOR);
		}
	}

}
