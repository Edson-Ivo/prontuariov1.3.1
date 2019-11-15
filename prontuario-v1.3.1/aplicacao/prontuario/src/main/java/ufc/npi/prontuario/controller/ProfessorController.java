package ufc.npi.prontuario.controller;

import static ufc.npi.prontuario.util.ConfigurationConstants.PERMISSAO_ADMINISTRACAO;
import static ufc.npi.prontuario.util.ConfigurationConstants.PERMISSAO_ADMINISTRACAO_VERIFICACAO_ID_PROFESSOR;
import static ufc.npi.prontuario.util.ExceptionSuccessConstants.ERROR;
import static ufc.npi.prontuario.util.ExceptionSuccessConstants.ERRO_ADICIONAR_SERVIDOR_PAPEL;
import static ufc.npi.prontuario.util.ExceptionSuccessConstants.ERRO_ADICIONAR_SERVIDOR_VAZIO;
import static ufc.npi.prontuario.util.ExceptionSuccessConstants.SUCCESS;
import static ufc.npi.prontuario.util.ExceptionSuccessConstants.SUCCESS_ATUALIZAR_SERVIDOR;
import static ufc.npi.prontuario.util.ExceptionSuccessConstants.SUCCESS_CADASTRAR_SERVIDOR;
import static ufc.npi.prontuario.util.ExceptionSuccessConstants.SUCCESS_EXCLUIR_SERVIDOR;
import static ufc.npi.prontuario.util.PagesConstants.DETALHES_PROFESSOR;
import static ufc.npi.prontuario.util.PagesConstants.FORMULARIO_CADASTRAR_PROFESSOR;
import static ufc.npi.prontuario.util.PagesConstants.LISTAGEM_PROFESSOR;
import static ufc.npi.prontuario.util.RedirectConstants.REDIRECT_FORMULARIO_CADASTRAR_PROFESSOR;
import static ufc.npi.prontuario.util.RedirectConstants.REDIRECT_LISTAGEM_PROFESSOR;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ufc.npi.prontuario.exception.ProntuarioException;
import ufc.npi.prontuario.model.Papel;
import ufc.npi.prontuario.model.Servidor;
import ufc.npi.prontuario.service.AtendimentoService;
import ufc.npi.prontuario.service.ServidorService;
import ufc.npi.prontuario.service.TurmaService;

@Controller
@RequestMapping("/professor")
public class ProfessorController {

	@Autowired
	private ServidorService servidorService;

	@Autowired
	private TurmaService turmaService;

	@Autowired
	private AtendimentoService atendimentoService;

	@PreAuthorize(PERMISSAO_ADMINISTRACAO)
	@GetMapping("/cadastrar")
	public ModelAndView formAdicionarProfessor(Servidor servidor) {
		servidor.addPapel(Papel.PROFESSOR);
		return new ModelAndView(FORMULARIO_CADASTRAR_PROFESSOR).addObject("servidor", servidor)
				.addObject("papeis", Arrays.asList(Papel.PROFESSOR, Papel.ADMINISTRACAO, Papel.ATENDENTE));
	}

	@PreAuthorize(PERMISSAO_ADMINISTRACAO)
	@PostMapping("/cadastrar")
	public ModelAndView adicionarProfessor(Servidor professor, RedirectAttributes attributes) {
		ModelAndView modelAndView = new ModelAndView(REDIRECT_LISTAGEM_PROFESSOR);
		try {
			if(professor.getId()!= null && professor.getPapeis().size() == 0){
				
				modelAndView.setViewName(REDIRECT_FORMULARIO_CADASTRAR_PROFESSOR);
				attributes.addFlashAttribute("servidor", professor);
				attributes.addFlashAttribute(ERROR, ERRO_ADICIONAR_SERVIDOR_PAPEL);
			}
			
			if(professor.getId()!= null && professor.getNome().replaceAll(" ", "").length() == 0){
				modelAndView.setViewName(REDIRECT_FORMULARIO_CADASTRAR_PROFESSOR);
				attributes.addFlashAttribute("servidor", servidorService.buscarPorId(professor.getId()));
				attributes.addFlashAttribute(ERROR, ERRO_ADICIONAR_SERVIDOR_VAZIO);
			}
			
			if(professor.getId() == null) {
				servidorService.salvar(professor);
				attributes.addFlashAttribute(SUCCESS, SUCCESS_CADASTRAR_SERVIDOR);
			} else {
				servidorService.atualizar(professor);
				attributes.addFlashAttribute(SUCCESS, SUCCESS_ATUALIZAR_SERVIDOR);
			}
		} catch (ProntuarioException e) {
			attributes.addFlashAttribute(ERROR,e.getMessage());
			modelAndView.setViewName(REDIRECT_FORMULARIO_CADASTRAR_PROFESSOR);

		}
		return modelAndView;
	}

	@PreAuthorize(PERMISSAO_ADMINISTRACAO)
	@GetMapping("/editar/{id}")
	public ModelAndView formEditarProfessor(@PathVariable("id") Servidor professor, RedirectAttributes attributes) {
		return new ModelAndView(FORMULARIO_CADASTRAR_PROFESSOR).addObject("servidor", professor)
				.addObject("papeis", Arrays.asList(Papel.PROFESSOR, Papel.ADMINISTRACAO, Papel.ATENDENTE));
	}

	@PreAuthorize(PERMISSAO_ADMINISTRACAO)
	@GetMapping(value = "/listar")
	public ModelAndView listarProfessores() {
		ModelAndView modelAndView = new ModelAndView(LISTAGEM_PROFESSOR);
		modelAndView.addObject("professores", servidorService.buscarProfessores());
		return modelAndView;
	}

	@PreAuthorize(PERMISSAO_ADMINISTRACAO)
	@GetMapping(value = "/remover/{id}")
	public ModelAndView excluirServidor(@PathVariable("id") Servidor servidor, RedirectAttributes attributes) {
		
		try {
			servidorService.removerServidor(servidor.getId());
			attributes.addFlashAttribute(SUCCESS, SUCCESS_EXCLUIR_SERVIDOR);
		} catch (ProntuarioException e) {
			attributes.addFlashAttribute(ERROR, e.getMessage());
		}
		
		return new ModelAndView(REDIRECT_LISTAGEM_PROFESSOR);
	}

	@PreAuthorize(PERMISSAO_ADMINISTRACAO_VERIFICACAO_ID_PROFESSOR)
	@GetMapping("/detalhes/{id}")
	public ModelAndView detalhes(@PathVariable("id") Servidor professor, Authentication authentication) {
		ModelAndView mv = new ModelAndView(DETALHES_PROFESSOR);
		mv.addObject("professor", professor);
		mv.addObject("turmas", turmaService.buscarTurmasProfessor(professor));

		if (professor.equals((Servidor) authentication.getPrincipal())) {
			mv.addObject("atendimentos", atendimentoService.buscarAtendimentosNaoFinalizadosPorProfessor(professor));
		}

		return mv;
	}
}
