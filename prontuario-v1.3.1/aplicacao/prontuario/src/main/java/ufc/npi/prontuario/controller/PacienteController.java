package ufc.npi.prontuario.controller;

import static ufc.npi.prontuario.util.ConfigurationConstants.PERMISSAO_ATENDENTE;
import static ufc.npi.prontuario.util.ConfigurationConstants.PERMISSAO_ESTUDANTE;
import static ufc.npi.prontuario.util.ConfigurationConstants.PERMISSOES_ESTUDANTE_PROFESSOR_ADMINISTRACAO;
import static ufc.npi.prontuario.util.ExceptionSuccessConstants.ERROR;
import static ufc.npi.prontuario.util.ExceptionSuccessConstants.SUCCESS;
import static ufc.npi.prontuario.util.ExceptionSuccessConstants.SUCCESS_CADASTRAR_PACIENTE;
import static ufc.npi.prontuario.util.ExceptionSuccessConstants.SUCCESS_CADASTRAR_TRATAMENTO;
import static ufc.npi.prontuario.util.ExceptionSuccessConstants.SUCCESS_EDITAR_PACIENTE;
import static ufc.npi.prontuario.util.ExceptionSuccessConstants.SUCCESS_EXCLUIR_TRATAMENTO;
import static ufc.npi.prontuario.util.ExceptionSuccessConstants.SUCCESS_REALIZAR_ANAMNESE;
import static ufc.npi.prontuario.util.PagesConstants.FORMULARIO_CADASTRO_PACIENTE;
import static ufc.npi.prontuario.util.PagesConstants.FORMULARIO_REALIZAR_ANAMNESE;
import static ufc.npi.prontuario.util.PagesConstants.PAGINA_CADASTRO_PLANO_TRATAMENTO;
import static ufc.npi.prontuario.util.PagesConstants.PAGINA_DETALHES_ANAMNESE_PACIENTE;
import static ufc.npi.prontuario.util.PagesConstants.PAGINA_DETALHES_PACIENTE;
import static ufc.npi.prontuario.util.PagesConstants.PAGINA_LISTAGEM_ANAMNESES_PACIENTE;
import static ufc.npi.prontuario.util.PagesConstants.PAGINA_LISTAGEM_ATENDIMENTOS;
import static ufc.npi.prontuario.util.PagesConstants.PAGINA_LISTAGEM_PACIENTES;
import static ufc.npi.prontuario.util.PagesConstants.PAGINA_LISTAGEM_TRATAMENTOS;
import static ufc.npi.prontuario.util.PagesConstants.TABLE_ATENDIMENTOS;
import static ufc.npi.prontuario.util.PagesConstants.TABLE_LISTAGEM_TRATAMENTOS;
import static ufc.npi.prontuario.util.RedirectConstants.REDIRECT_INDEX;
import static ufc.npi.prontuario.util.RedirectConstants.REDIRECT_LISTAGEM_PACIENTES;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ufc.npi.prontuario.exception.ProntuarioException;
import ufc.npi.prontuario.model.Anamnese;
import ufc.npi.prontuario.model.Paciente;
import ufc.npi.prontuario.model.Paciente.Estado;
import ufc.npi.prontuario.model.Paciente.EstadoCivil;
import ufc.npi.prontuario.model.Paciente.Sexo;
import ufc.npi.prontuario.model.PacienteAnamnese;
import ufc.npi.prontuario.model.PlanoTratamento;
import ufc.npi.prontuario.model.Servidor;
import ufc.npi.prontuario.model.Usuario;
import ufc.npi.prontuario.service.AlunoService;
import ufc.npi.prontuario.service.AnamneseService;
import ufc.npi.prontuario.service.AtendimentoService;
import ufc.npi.prontuario.service.DisciplinaService;
import ufc.npi.prontuario.service.PacienteService;
import ufc.npi.prontuario.service.PlanoTratamentoService;

@Controller
@RequestMapping("/paciente")
public class PacienteController {

	@Autowired
	private PacienteService pacienteService;

	@Autowired
	private AnamneseService anamneseService;

	@Autowired
	private AlunoService alunoService;

	@Autowired
	private AtendimentoService atendimentoService;

	@Autowired
	private DisciplinaService disciplinaService;

	@Autowired
	private PlanoTratamentoService tratamentoService;

	@GetMapping
	public ModelAndView listagemPacientes() {
		ModelAndView modelAndView = new ModelAndView(PAGINA_LISTAGEM_PACIENTES);

		modelAndView.addObject("pacientes", pacienteService.buscarTudo());

		return modelAndView;
	}

	@GetMapping("/cadastrar")
	public ModelAndView paginaCadastroPaciente(Paciente paciente) {
		ModelAndView modelAndView = new ModelAndView(FORMULARIO_CADASTRO_PACIENTE);

		modelAndView.addObject("sexo", Sexo.values());
		modelAndView.addObject("estado", Estado.values());
		modelAndView.addObject("estadoCivil", EstadoCivil.values());
		modelAndView.addObject("raca", Paciente.Raca.values());
		modelAndView.addObject("action", "cadastrar");

		return modelAndView;
	}

	@PostMapping("/cadastrar")
	public ModelAndView cadastrarPaciente(Paciente paciente, RedirectAttributes attributes) {

		ModelAndView modelAndView = new ModelAndView(REDIRECT_LISTAGEM_PACIENTES);

		try {
			pacienteService.salvar(paciente);
			attributes.addFlashAttribute(SUCCESS, SUCCESS_CADASTRAR_PACIENTE);
		} catch (ProntuarioException e) {
			modelAndView.addObject("sexo", Sexo.values());
			modelAndView.addObject("estado", Estado.values());
			modelAndView.addObject("estadoCivil", EstadoCivil.values());
			modelAndView.addObject("raca", Paciente.Raca.values());
			modelAndView.addObject("action", "cadastrar");
			modelAndView.addObject(ERROR, e.getMessage());
			modelAndView.setViewName(FORMULARIO_CADASTRO_PACIENTE);
		}

		return modelAndView;
	}

	@GetMapping("/editar/{idPaciente}")
	public ModelAndView editarPaciente(@PathVariable("idPaciente") Paciente paciente) {
		ModelAndView modelAndView = new ModelAndView(FORMULARIO_CADASTRO_PACIENTE);

		modelAndView.addObject("paciente", paciente);
		modelAndView.addObject("sexo", Sexo.values());
		modelAndView.addObject("estado", Estado.values());
		modelAndView.addObject("estadoCivil", EstadoCivil.values());
		modelAndView.addObject("raca", Paciente.Raca.values());
		modelAndView.addObject("action", "editar");

		return modelAndView;
	}

	@PostMapping("/editar")
	public ModelAndView editarPaciente(Paciente paciente, RedirectAttributes attributes) {

		ModelAndView modelAndView = new ModelAndView(REDIRECT_LISTAGEM_PACIENTES);
		try {
			pacienteService.salvar(paciente);
			attributes.addFlashAttribute(SUCCESS, SUCCESS_EDITAR_PACIENTE);
		} catch (ProntuarioException e) {
			modelAndView.addObject("sexo", Sexo.values());
			modelAndView.addObject("estado", Estado.values());
			modelAndView.addObject("estadoCivil", EstadoCivil.values());
			modelAndView.addObject("raca", Paciente.Raca.values());
			modelAndView.addObject("action", "editar");
			modelAndView.addObject(ERROR, e.getMessage());
			modelAndView.setViewName(FORMULARIO_CADASTRO_PACIENTE);
		}

		return modelAndView;
	}

	@GetMapping("/{idPaciente}")
	public ModelAndView visualizarDetalhes(@PathVariable("idPaciente") Paciente paciente) {
		ModelAndView modelAndView = new ModelAndView(PAGINA_DETALHES_PACIENTE);
		modelAndView.addObject("paciente", paciente);
		return modelAndView;
	}

	@PreAuthorize(PERMISSOES_ESTUDANTE_PROFESSOR_ADMINISTRACAO)
	@GetMapping("/{idPaciente}/anamneses")
	public ModelAndView listarAnamneses(@PathVariable("idPaciente") Paciente paciente) {
		ModelAndView modelAndView = new ModelAndView(PAGINA_LISTAGEM_ANAMNESES_PACIENTE);
		modelAndView.addObject("paciente", paciente);
		modelAndView.addObject("anamneses", anamneseService.buscarTodasFinalizadas());
		modelAndView.addObject("pacienteAnamneses", paciente.getPacienteAnamneses());
		return modelAndView;
	}

	@PreAuthorize(PERMISSOES_ESTUDANTE_PROFESSOR_ADMINISTRACAO)
	@GetMapping("/{idPaciente}/atendimentos")
	public ModelAndView listarAtendimentos(@PathVariable("idPaciente") Paciente paciente, Authentication auth) {
		ModelAndView modelAndView = new ModelAndView(PAGINA_LISTAGEM_ATENDIMENTOS);
		Usuario usuario = (Usuario) auth.getPrincipal();
		modelAndView.addObject("paciente", paciente);
		modelAndView.addObject("atendimentos",
				atendimentoService.buscarAtendimentosPorUsuario(usuario.getId(), paciente));
		return modelAndView;
	}

	@PreAuthorize(PERMISSOES_ESTUDANTE_PROFESSOR_ADMINISTRACAO)
	@GetMapping("/{idPaciente}/tableAtendimentos")
	public ModelAndView getTableAtendimentos(@PathVariable("idPaciente") Paciente paciente, Authentication auth) {
		ModelAndView modelAndView = new ModelAndView(TABLE_ATENDIMENTOS);
		Usuario usuario = (Usuario) auth.getPrincipal();
		modelAndView.addObject("paciente", paciente);
		modelAndView.addObject("atendimentos",
				atendimentoService.buscarAtendimentosPorUsuario(usuario.getId(), paciente));
		return modelAndView;
	}

	@PreAuthorize(PERMISSAO_ESTUDANTE)
	@GetMapping("/{idPaciente}/anamnese")
	public ModelAndView realizarAnamneseForm(@PathVariable("idPaciente") Paciente paciente,
			@RequestParam("idAnamnese") Anamnese anamnese, PacienteAnamnese pacienteAnamnese) {
		ModelAndView modelAndView = new ModelAndView(FORMULARIO_REALIZAR_ANAMNESE);

		modelAndView.addObject("paciente", paciente);
		modelAndView.addObject("anamnese", anamnese);
		modelAndView.addObject("pacienteAnamnese", pacienteAnamnese);

		return modelAndView;
	}

	@PreAuthorize(PERMISSAO_ESTUDANTE)
	@PostMapping("/{idPaciente}/anamnese")
	public ModelAndView realizarAnamnese(@PathVariable("idPaciente") Paciente paciente,
			PacienteAnamnese pacienteAnamnese, Authentication auth, RedirectAttributes attributes) {

		Usuario usuario = (Usuario) auth.getPrincipal();
		
		pacienteAnamnese.setResponsavel(alunoService.buscarPorMatricula(usuario.getMatricula()));
		pacienteService.adicionarAnamnese(paciente, pacienteAnamnese);

		attributes.addFlashAttribute(SUCCESS, SUCCESS_REALIZAR_ANAMNESE);

		return new ModelAndView("redirect:/paciente/" + paciente.getId() + "/anamneses");
	}

	@PreAuthorize(PERMISSOES_ESTUDANTE_PROFESSOR_ADMINISTRACAO)
	@GetMapping("/{idPaciente}/anamnese/{idAnamnese}")
	public ModelAndView visualizarDetalhesAnamnese(@PathVariable("idPaciente") Paciente paciente,
			@PathVariable("idAnamnese") PacienteAnamnese anamnese) {
		ModelAndView modelAndView = new ModelAndView(PAGINA_DETALHES_ANAMNESE_PACIENTE);
		modelAndView.addObject("paciente", paciente);
		modelAndView.addObject("anamnese", anamnese);
		return modelAndView;
	}

	// @PreAuthorize(PERMISSOES_ESTUDANTE_PROFESSOR)
	@GetMapping("/{idPaciente}/tratamentos")
	public ModelAndView listarTratamentos(@PathVariable("idPaciente") Paciente paciente) {
		ModelAndView modelAndView = new ModelAndView(PAGINA_LISTAGEM_TRATAMENTOS);
		modelAndView.addObject("paciente", paciente);
		modelAndView.addObject("tratamentos", paciente.getTratamentos());
		return modelAndView;
	}

	@PreAuthorize(PERMISSAO_ATENDENTE)
	@GetMapping("/plano-tratamento/{idPaciente}/cadastrar")
	public ModelAndView paginaCadastro(@PathVariable("idPaciente") Paciente paciente) {
		ModelAndView mv = new ModelAndView(PAGINA_CADASTRO_PLANO_TRATAMENTO);
		mv.addObject("paciente", paciente);
		mv.addObject("clinicas", disciplinaService.buscarTudo());
		mv.addObject("tratamento", new PlanoTratamento());
		return mv;
	}

	@PreAuthorize(PERMISSAO_ATENDENTE)
	@PostMapping("/plano-tratamento/cadastrar")
	public ModelAndView cadastrar(PlanoTratamento planoTratamento, @RequestParam("paciente") Paciente paciente,
			@RequestParam("responsavel") Servidor responsavel, RedirectAttributes attributes) {
		String redirectTratamentos = REDIRECT_INDEX + "paciente/" + paciente.getId() + "/tratamentos";
		ModelAndView mv = new ModelAndView(redirectTratamentos);
		try {
			tratamentoService.salvar(planoTratamento, responsavel, paciente);
			attributes.addFlashAttribute(SUCCESS, SUCCESS_CADASTRAR_TRATAMENTO);
		} catch (ProntuarioException e) {
			attributes.addFlashAttribute(ERROR, e.getMessage());
		}
		return mv;
	}

	@PreAuthorize(PERMISSAO_ATENDENTE)
	@PostMapping("/plano-tratamento/{idPaciente}/excluir/{tratamento}")
	public ModelAndView excluir(@PathVariable("tratamento") PlanoTratamento planoTratamento,
			@PathVariable("idPaciente") Paciente paciente, RedirectAttributes attributes) {
		ModelAndView mv = new ModelAndView(TABLE_LISTAGEM_TRATAMENTOS);

		try {
			tratamentoService.excluirPlanoTratamento(planoTratamento);
			attributes.addFlashAttribute(SUCCESS, SUCCESS_EXCLUIR_TRATAMENTO);
		} catch (ProntuarioException e) {
			attributes.addFlashAttribute(ERROR, e.getMessage());
		}

		mv.addObject("paciente", paciente);
		mv.addObject("tratamentos", paciente.getTratamentos());

		return mv;
	}
}
