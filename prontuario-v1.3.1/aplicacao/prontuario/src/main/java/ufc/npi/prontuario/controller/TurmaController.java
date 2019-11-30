package ufc.npi.prontuario.controller;

import static ufc.npi.prontuario.util.ConfigurationConstants.PERMISSAO_ADMINISTRACAO;
import static ufc.npi.prontuario.util.ConfigurationConstants.PERMISSOES_ADMINISTRACAO_VERIFICACAO_PROFESSOR;
import static ufc.npi.prontuario.util.ConfigurationConstants.PERMISSOES_PROFESSOR_ADMINISTRACAO;
import static ufc.npi.prontuario.util.ExceptionSuccessConstants.ERROR;
import static ufc.npi.prontuario.util.ExceptionSuccessConstants.SUCCESS;
import static ufc.npi.prontuario.util.ExceptionSuccessConstants.SUCCESS_ALTERAR_STATUS;
import static ufc.npi.prontuario.util.ExceptionSuccessConstants.SUCCESS_CADASTRAR_TURMA;
import static ufc.npi.prontuario.util.ExceptionSuccessConstants.SUCCESS_EXCLUIR_TURMA;
import static ufc.npi.prontuario.util.FragmentsConstants.FRAGMENT_STATUS_TURMA;
import static ufc.npi.prontuario.util.PagesConstants.PAGINA_DETALHES_TURMA;
import static ufc.npi.prontuario.util.PagesConstants.PAGINA_LISTAGEM_TURMAS;
import static ufc.npi.prontuario.util.RedirectConstants.REDIRECT_DETALHES_TURMA;
import static ufc.npi.prontuario.util.RedirectConstants.REDIRECT_LISTAGEM_TURMA;

import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ufc.npi.prontuario.exception.ProntuarioException;
import ufc.npi.prontuario.model.Servidor;
import ufc.npi.prontuario.model.Turma;
import ufc.npi.prontuario.model.Usuario;
import ufc.npi.prontuario.service.TurmaService;

@Controller
@RequestMapping("/turma")
public class TurmaController {

	@Autowired
	private TurmaService turmaService;

	@Autowired
	private ProfessorTurmaController profTurmaController;

	@PreAuthorize(PERMISSOES_PROFESSOR_ADMINISTRACAO)
	@GetMapping(value = "/listar")
	public ModelAndView listarTurmas(Authentication auth) {
		ModelAndView modelAndView = new ModelAndView(PAGINA_LISTAGEM_TURMAS);
		modelAndView.addObject("turmas", turmaService.buscarTurmas((Servidor) auth.getPrincipal()));
		return modelAndView;
	}

	@PreAuthorize(PERMISSAO_ADMINISTRACAO)
	@PostMapping("/adicionar")
	public ModelAndView adicionarTurma(@Valid @ModelAttribute Turma turma, BindingResult result,
			Authentication auth, RedirectAttributes attributes) {
		
		if(result.hasErrors()){
			listarTurmas(auth);
		}

		try {
			turmaService.salvar(turma);
			attributes.addFlashAttribute(SUCCESS, SUCCESS_CADASTRAR_TURMA);
		} catch(ProntuarioException e) {
			attributes.addFlashAttribute(ERROR, e.getMessage());
			return profTurmaController.formularioAdicionarTurma(turma);
		}

		return new ModelAndView(REDIRECT_DETALHES_TURMA + turma.getId());
	}

	@PreAuthorize(PERMISSAO_ADMINISTRACAO)
	@PostMapping("/{idTurma}/inscrever")
	public ModelAndView inscreverAluno(@PathVariable("idTurma") Turma turma,
			@RequestParam("matricula") String matricula, RedirectAttributes attributes) {
		
		try {
			turmaService.inscreverAluno(turma, matricula);
			attributes.addFlashAttribute(SUCCESS, SUCCESS_MATRICULAR_ALUNO);
		} catch (ProntuarioException e) {
			attributes.addFlashAttribute(ERROR, e.getMessage());
		}
		
		return new ModelAndView(REDIRECT_DETALHES_TURMA + turma.getId());
	}

	@PreAuthorize(PERMISSAO_ADMINISTRACAO)
	@PostMapping("/{idTurma}/adicionar-professor")
	public ModelAndView adicionarProfessor(@PathVariable("idTurma") Turma turma, 
			@ModelAttribute("novosProfessores") Turma novosProfessores, RedirectAttributes attributes){
		List<Servidor> professores = novosProfessores.getProfessores();
		
		if(listIsNotNullOrEmpty(professores)){
			turmaService.adicionarProfessorTurma(turma, professores);
			attributes.addFlashAttribute(SUCCESS, SUCCESS_VINCULAR_PROFESSOR);
		}
		
		return new ModelAndView(REDIRECT_DETALHES_TURMA + turma.getId());
	}
	
	private boolean listIsNotNullOrEmpty(List<?> list) {
		return list != null && !(list.isEmpty());
	}
	
	@PostAuthorize(PERMISSOES_ADMINISTRACAO_VERIFICACAO_PROFESSOR)
	@GetMapping("/{idTurma}")
	public ModelAndView visualizarDetalhes(@PathVariable("idTurma") Turma turma) {
		ModelAndView modelAndView = new ModelAndView(PAGINA_DETALHES_TURMA);
		
		List<Usuario> listaProfessores = turmaService.buscarProfessores(turma.getProfessores());
		
		modelAndView.addObject("turma", turma);
		modelAndView.addObject("lista_professores", listaProfessores);
		modelAndView.addObject("novosProfessores", new Turma());
		return modelAndView;
	}
	
	@PreAuthorize(PERMISSAO_ADMINISTRACAO)
	@GetMapping(value = "/remover/{idTurma}")
	public ModelAndView excluirTurma(@PathVariable("idTurma") Turma turma, RedirectAttributes attributes) {
		ModelAndView modelAndView = new ModelAndView(REDIRECT_LISTAGEM_TURMA);
		try{
			turmaService.removerTurma(turma.getId());
			attributes.addFlashAttribute(SUCCESS, SUCCESS_EXCLUIR_TURMA);
		} catch(ProntuarioException e) {
			attributes.addFlashAttribute(ERROR, e.getMessage());
			return modelAndView;
		}
		return modelAndView;
	}
	
	@PostMapping("/{idTurma}/status")
	public ModelAndView alterarStatus(@PathVariable("idTurma") Turma turma, RedirectAttributes attributes) {
		ModelAndView modelAndView = new ModelAndView(FRAGMENT_STATUS_TURMA);
		turmaService.alterarStatus(turma);
		modelAndView.addObject("turma", turma);
		attributes.addFlashAttribute(SUCCESS, SUCCESS_ALTERAR_STATUS);
		return modelAndView;
	}
	
}
