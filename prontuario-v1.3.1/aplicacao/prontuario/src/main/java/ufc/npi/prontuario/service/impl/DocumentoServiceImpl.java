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
public class DocumentoServiceImpl implements DocumentoService {

	@Value("${documents.folder}")
	private String DOCUMENTOS_PRONTUARIO;

	@Autowired
	DocumentoRepository documentoRepository;

	@Autowired
	private PacienteRepository pacienteRepository;

	@Override
	public void salvar(Paciente paciente, MultipartFile[] files) throws ProntuarioException {
		for (MultipartFile file : files) {
			if (file != null && !(file.getOriginalFilename().toString().equals(""))) {
				Documento documento = new Documento();
				try {
					documento.setNome(file.getOriginalFilename());
					documento.setArquivo(file.getBytes());
					documento.setCaminho(DOCUMENTOS_PRONTUARIO + paciente.getId() + "/" + documento.getNome());

					// pega o fomato do arquivo
					int i = documento.getNome().lastIndexOf('.');
					String extensao = documento.getNome().substring(i + 1);
					documento.setTipo(TipoDocumento.valueOf(extensao.toUpperCase()));

					for (Documento doc : paciente.getDocumentos()) {
						if (documento.getCaminho().equals(doc.getCaminho())) {
							throw new ProntuarioException(ERRO_ARQUIVO_EXISTENTE);
						}
					}

					paciente.addDocumento(documento);
					pacienteRepository.save(paciente);
				} catch (IOException e) {
					throw new ProntuarioException(ERRO_SALVAR_ARQUIVO);
				}
				salvarArquivoLocal(documento, paciente.getId());
			}
		}
	}

	private void salvarArquivoLocal(Documento documento, Integer idPaciente) throws ProntuarioException {
		String caminhoDiretorio = DOCUMENTOS_PRONTUARIO + idPaciente;

		try {
			File arquivo = criarArquivo(caminhoDiretorio, documento.getNome());

			FileOutputStream fileOutputStream = new FileOutputStream(arquivo);
			fileOutputStream.write(documento.getArquivo());
			fileOutputStream.flush();
			fileOutputStream.close();
		} catch (IOException e) {
			throw new ProntuarioException(ERRO_SALVAR_ARQUIVO);
		}
	}

	private File criarArquivo(String caminhoDiretorio, String nomeArquivo) throws IOException {
		File diretorio = new File(caminhoDiretorio);
		diretorio.mkdirs();

		File arquivo = new File(diretorio, nomeArquivo);
		arquivo.createNewFile();

		return arquivo;
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

		String extensao;

		if (documento.getTipo().equals(TipoDocumento.PDF)) {
			extensao = "application/" + documento.getTipo().getDescricao();
		} else {
			extensao = "image/" + documento.getTipo().getDescricao();
		}

		return new DocumentoDownload(documento.getArquivo(), documento.getNome(), procedimento, extensao);
	}
}
