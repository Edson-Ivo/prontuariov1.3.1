package ufc.npi.prontuario.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ufc.npi.prontuario.model.Token;
import ufc.npi.prontuario.model.Usuario;
import ufc.npi.prontuario.repository.TokenRepository;
import ufc.npi.prontuario.service.TokenService;

@Service
public class TokenServiceImpl implements TokenService {

	@Autowired
	private TokenRepository tokenRepository;

	@Override
	public Token buscarPorUsuario(Usuario usuario) {
		return tokenRepository.findByUsuario(usuario);
	}

	@Override
	public Token buscar(String token) {
		return tokenRepository.findOne(token);
	}

	@Override
	public boolean existe(String token) {
		return tokenRepository.exists(token);
	}

	@Override
	public void salvar(Token token) {
		tokenRepository.save(token);
	}

	@Override
	public void deletar(Token token) {
		tokenRepository.delete(token);
	}

}
