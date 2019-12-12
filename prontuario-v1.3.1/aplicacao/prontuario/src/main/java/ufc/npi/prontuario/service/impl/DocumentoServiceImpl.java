package ufc.npi.prontuario.service.impl;

import static ufc.npi.prontuario.util.ExceptionSuccessConstants.ERRO_ARQUIVO_EXISTENTE;
import static ufc.npi.prontuario.util.ExceptionSuccessConstants.ERRO_CARREGAR_ARQUIVO;
import static ufc.npi.prontuario.util.ExceptionSuccessConstants.ERRO_SALVAR_ARQUIVO;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import ufc.npi.prontuario.exception.ProntuarioException;
import ufc.npi.prontuario.model.Documento;
import ufc.npi.prontuario.model.Documento.TipoDocumento;
import ufc.npi.prontuario.model.DocumentoDownload;
import ufc.npi.prontuario.model.Paciente;
import ufc.npi.prontuario.repository.DocumentoRepository;
import ufc.npi.prontuario.repository.PacienteRepository;
import ufc.npi.prontuario.service.DocumentoService;

@Service
public class DocumentoServiceImpl implements DocumentoService{
	
	@Value("${documents.folder}")
	private String DOCUMENTOS_PRONTUARIO;
	
	@Autowired
	DocumentoRepository documentoRepository;
	
	@Autowired
	private PacienteRepository pacienteRepository;

	@Override
	public void salvar(Paciente paciente, MultipartFile[] files) throws ProntuarioException {
		for(MultipartFile file : files) {
			if(file != null && !(file.getOriginalFilename().toString().equals(""))) {
				Documento documento = new Documento();
				try {
					documento.setNome(file.getOriginalFilename());
					documento.setArquivo(file.getBytes());
					documento.setCaminho(DOCUMENTOS_PRONTUARIO + paciente.getId() + "/" + documento.getNome());
					
					//pega  o fomato do arquivo
					int i = documento.getNome().lastIndexOf('.');
					String extensao = documento.getNome().substring(i+1);
					documento.setTipo(TipoDocumento.valueOf(extensao.toUpperCase()));
					
					for(Documento doc : paciente.getDocumentos()) {
						if(documento.getCaminho().equals(doc.getCaminho())) {
							throw new ProntuarioException(ERRO_ARQUIVO_EXISTENTE);
						}
					}
					
					paciente.addDocumento(documento);
					pacienteRepository.save(paciente);
				} catch (IOException e) {
					throw new ProntuarioException(ERRO_SALVAR_ARQUIVO);
				}
				salvarArquivoLocal(documento, paciente);
			}
		}
	}
	
	private void salvarArquivoLocal(Documento documento, Paciente paciente) throws ProntuarioException {
		String caminhoDiretorio = DOCUMENTOS_PRONTUARIO + paciente.getId();
		File diretorio = new File(caminhoDiretorio);
		diretorio.mkdirs();
		
		try{
			File arquivo = new File(diretorio, documento.getNome());
			FileOutputStream fop = new FileOutputStream(arquivo);
			arquivo.createNewFile();
			fop.write(documento.getArquivo());
			fop.flush();
			fop.close();
		} catch (IOException e) {
			throw new ProntuarioException(ERRO_SALVAR_ARQUIVO);
		}
	}

	public Documento buscarArquivo(Documento documento) throws ProntuarioException {
		FileInputStream fileInputStream = null;
		File file = new File(documento.getCaminho());
		byte[] bFile = new byte[(int) file.length()];

		try {
			fileInputStream = new FileInputStream(file);
			fileInputStream.read(bFile);
			fileInputStream.close();
		} catch (IOException e) {
			throw new ProntuarioException(ERRO_CARREGAR_ARQUIVO);
		}
		
		documento.setArquivo(bFile);
		return documento;
	}

	public void deletar(Documento documento, Paciente paciente) {
		File file = new File(documento.getCaminho());
		file.delete();
		
		paciente.removerDocumento(documento);
		pacienteRepository.save(paciente);
		documentoRepository.delete(documento);
	}

	@Override
	public DocumentoDownload downloadDocumento(Documento documento, String procedimento) {
		String extensao = getExtensaoDocumento(documento);
		
		return new DocumentoDownload(documento.getArquivo(), documento.getNome(), procedimento, extensao);
	}

	private String getExtensaoDocumento(Documento documento) {
		String extensao = "";

		if(documento.getTipo().equals(TipoDocumento.PDF)) {
			extensao = "application/" + documento.getTipo().getDescricao();
		} else {
			extensao = "image/" + documento.getTipo().getDescricao();
		}

		return extensao;
	}
}
