package ufc.npi.prontuario.service.impl;

import static ufc.npi.prontuario.util.ExceptionSuccessConstants.ERRO_CAMPOS_OBRIGATORIOS;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ufc.npi.prontuario.exception.ProntuarioException;
import ufc.npi.prontuario.model.TipoProcedimento;
import ufc.npi.prontuario.repository.TipoProcedimentoRepository;
import ufc.npi.prontuario.service.TipoProcedimentoService;
import ufc.npi.prontuario.util.ExceptionSuccessConstants;

@Service
public class TipoProcedimentoServiceImpl implements TipoProcedimentoService{
	
	@Autowired
	private TipoProcedimentoRepository tipoProcedimentoRepository;

	
	@Override
	public void salvar(TipoProcedimento tipoProcedimento) throws ProntuarioException {
		if(tipoProcedimento.getNome().trim().isEmpty() || tipoProcedimento.getDescricao().trim().isEmpty()) {
			throw new ProntuarioException(ERRO_CAMPOS_OBRIGATORIOS);
		}
		if(tipoProcedimentoRepository.findByNome(tipoProcedimento.getNome()) != null) {
			throw new ProntuarioException(ExceptionSuccessConstants.ERRO_SALVAR_TIPO_PROCEDIMENTO_EXISTENTE);
		}
		tipoProcedimentoRepository.save(tipoProcedimento);
	}

	@Override
	public void atualizar(TipoProcedimento tipoProcedimento) throws ProntuarioException {
		if (tipoProcedimento.getId() == null || tipoProcedimento.getNome().trim().isEmpty()) {
			throw new ProntuarioException(ExceptionSuccessConstants.ERRO_CAMPOS_OBRIGATORIOS);
		}

		TipoProcedimento tipoProcedimentoExistente = tipoProcedimentoRepository.findByNome(tipoProcedimento.getNome());
		if (tipoProcedimentoExistente != null && !tipoProcedimentoExistente.getId().equals(tipoProcedimento.getId())) {
			throw new ProntuarioException(ExceptionSuccessConstants.ERRO_SALVAR_TIPO_PATOLOGIA_EXISTENTE);
		}

		tipoProcedimentoRepository.save(tipoProcedimento);
	}

	@Override
	public List<TipoProcedimento> buscarTudo() {
		return tipoProcedimentoRepository.findAll();
	}

	@Override
	public TipoProcedimento buscarPorId(Integer id) {
		return tipoProcedimentoRepository.findOne(id);
	}

	@Override
	public void remover(Integer id) throws Exception {
		tipoProcedimentoRepository.delete(id);
	}

	@Override
	public List<TipoProcedimento> buscarPorNome(String query) {
		return tipoProcedimentoRepository.findByNomeContainingIgnoreCase(query);
	}

}
