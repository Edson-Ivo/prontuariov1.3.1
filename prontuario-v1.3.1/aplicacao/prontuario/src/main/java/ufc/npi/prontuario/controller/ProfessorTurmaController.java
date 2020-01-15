package ufc.npi.prontuario.controller;

import static ufc.npi.prontuario.util.ConfigurationConstants.PERMISSAO_ADMINISTRACAO;
import static ufc.npi.prontuario.util.ExceptionSuccessConstants.ERROR;
import static ufc.npi.prontuario.util.ExceptionSuccessConstants.SUCCESS;
import static ufc.npi.prontuario.util.ExceptionSuccessConstants.SUCCESS_DESVINCULAR_PROFESSOR;
import static ufc.npi.prontuario.util.FragmentsConstants.FRAGMENT_PROFESSORES;
import static ufc.npi.prontuario.util.PagesConstants.FORMULARIO_ADICIONAR_TURMA;
import static ufc.npi.prontuario.util.RedirectConstants.REDIRECT_DETALHES_TURMA;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ufc.npi.prontuario.exception.ProntuarioException;
import ufc.npi.prontuario.model.Servidor;
import ufc.npi.prontuario.model.Turma;
import ufc.npi.prontuario.service.DisciplinaService;
import ufc.npi.prontuario.service.ServidorService;
import ufc.npi.prontuario.service.TurmaService;

@Controller
@RequestMapping("/turma")
public class ProfessorTurmaController {

	@Autowired
	private TurmaService turmaService;
	
	@Autowired
	private DisciplinaService disciplinaService;

	@Autowired
	private ServidorService servidorService;
	
	@PreAuthorize(PERMISSAO_ADMINISTRACAO)
	@GetMapping("/adicionar")
	public ModelAndView formularioAdicionarTurma(Turma turma) {
		ModelAndView modelAndView = new ModelAndView(FORMULARIO_ADICIONAR_TURMA);
		modelAndView.addObject("professores", servidorService.buscarProfessores());
		modelAndView.addObject("disciplinas", disciplinaService.buscarTudo());
		return modelAndView;
	}
	
	@GetMapping("/professores/")
	public ModelAndView carregarProfessores(@RequestParam("idTurma") Turma turma ) {
		ModelAndView modelAndView = new ModelAndView(FRAGMENT_PROFESSORES);
		modelAndView.addObject("professores", turma != null ? turma.getProfessores() : null);
		return modelAndView;
	}
	
	@PreAuthorize(PERMISSAO_ADMINISTRACAO)
	@GetMapping("/{idTurma}/remover-professor/{idProfessor}")
	public ModelAndView removerProfessor(@PathVariable("idTurma") Turma turma, @PathVariable("idProfessor") Servidor professor,
			RedirectAttributes attributes) {
		
		try {
			turmaService.removerProfessor(turma, professor);
			attributes.addFlashAttribute(SUCCESS, SUCCESS_DESVINCULAR_PROFESSOR);
		} catch (ProntuarioException e) {
			attributes.addFlashAttribute(ERROR, e.getMessage());
		}
		
		return new ModelAndView(REDIRECT_DETALHES_TURMA + turma.getId());
	}
}
