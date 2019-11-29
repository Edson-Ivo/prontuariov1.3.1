package ufc.npi.prontuario.controller;

import static ufc.npi.prontuario.util.ConfigurationConstants.DATE_PATTERN;
import static ufc.npi.prontuario.util.ConfigurationConstants.PERMISSAO_ESTUDANTE;
import static ufc.npi.prontuario.util.ConfigurationConstants.PERMISSOES_ESTUDANTE_PROFESSOR_ADMINISTRACAO;
import static ufc.npi.prontuario.util.FragmentsConstants.FRAGMENT_MODAL_DETALHES_PATOLOGIA;
import static ufc.npi.prontuario.util.PagesConstants.PAGINA_ODONTOGRAMA;
import static ufc.npi.prontuario.util.PagesConstants.TABLE_PATOLOGIAS;
import static ufc.npi.prontuario.util.PagesConstants.TABLE_PROCEDIMENTOS;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import ufc.npi.prontuario.exception.ProntuarioException;
import ufc.npi.prontuario.model.Aluno;
import ufc.npi.prontuario.model.Odontograma;
import ufc.npi.prontuario.model.Paciente;
import ufc.npi.prontuario.model.Papel;
import ufc.npi.prontuario.model.Patologia;
import ufc.npi.prontuario.model.Procedimento;
import ufc.npi.prontuario.model.TipoPatologia;
import ufc.npi.prontuario.model.TipoProcedimento;
import ufc.npi.prontuario.model.Tratamento;
import ufc.npi.prontuario.model.Usuario;
import ufc.npi.prontuario.service.AlunoService;
import ufc.npi.prontuario.service.AtendimentoService;
import ufc.npi.prontuario.service.OdontogramaService;
import ufc.npi.prontuario.service.PatologiaService;
import ufc.npi.prontuario.service.ProcedimentoService;
import ufc.npi.prontuario.service.TipoPatologiaService;
import ufc.npi.prontuario.service.TipoProcedimentoService;

@Controller
@RequestMapping("/odontograma")
public class OdontogramaController {

	@Autowired
	private AlunoService alunoService;

	@Autowired
	private OdontogramaService odontogramaService;

	@Autowired
	private PatologiaService patologiaService;

	@Autowired
	private ProcedimentoService procedimentoService;

	@Autowired
	private TipoPatologiaService tipoPatologiaService;

	@Autowired
	private TipoProcedimentoService tipoProcedimentoService;

	@Autowired
	private AtendimentoService atendimentoService;

	@PreAuthorize(PERMISSOES_ESTUDANTE_PROFESSOR_ADMINISTRACAO)
	@GetMapping("/paciente/{id}")
	public ModelAndView novoOdontogramaPatologias(@PathVariable("id") Paciente paciente, Authentication auth) {
		ModelAndView mv = new ModelAndView(PAGINA_ODONTOGRAMA);
		mv.addObject("odontograma", odontogramaService.buscarPorPacienteId(paciente.getId()));
		mv.addObject("patologias", tipoPatologiaService.buscarTudo());
		mv.addObject("procedimentos", tipoProcedimentoService.buscarTudo());
		mv.addObject("paciente", paciente);
		
		Collection<? extends GrantedAuthority> papeis =  auth.getAuthorities();

		if (papeis.contains(Papel.ESTUDANTE)) {
			Aluno aluno = (Aluno) auth.getPrincipal();
			mv.addObject("existeAtendimento", atendimentoService.existeAtendimentoAbertoAlunoPaciente(aluno, paciente));
			mv.addObject("atendimento", atendimentoService.ultimoAtendimentoAbertoAlunoPaciente(aluno, paciente));
		}

		return mv;
	}

	@GetMapping("/tablePatologias/{id}")
	public ModelAndView tablePatologias(@PathVariable("id") Odontograma odontograma, Authentication auth) {
		ModelAndView mv = new ModelAndView(TABLE_PATOLOGIAS);
		Usuario usuario = (Usuario) auth.getPrincipal();
		mv.addObject("patologias", patologiaService.buscarPatologiasOdontograma(odontograma, usuario));
		if (usuario instanceof Aluno)
			mv.addObject("existeAtendimento", atendimentoService.existeAtendimentoAbertoAlunoPaciente((Aluno) usuario,
					odontograma.getPaciente()));
		return mv;
	}

	@GetMapping("/tableProcedimentos/{id}")
	public ModelAndView tableProcedimentos(@PathVariable("id") Odontograma odontograma, Authentication auth) {
		ModelAndView mv = new ModelAndView(TABLE_PROCEDIMENTOS);
		Usuario usuario = (Usuario) auth.getPrincipal();
		List<Procedimento> procedimentos = procedimentoService.buscarProcedimentosOdontograma(odontograma,
				usuario.getId());
		procedimentos
				.addAll(procedimentoService.buscarProcedimentosExistentesOdontograma(odontograma, usuario.getId()));
		procedimentos.sort((p1, p2) -> p1.getAtendimento().getData().compareTo(p1.getAtendimento().getData()));
		mv.addObject("procedimentos", procedimentos);
		return mv;
	}

	@PreAuthorize(PERMISSAO_ESTUDANTE)
	@PostMapping("/adicionarPatologia")
	public @ResponseBody Map<String, Object> adicionarPatologia(@ModelAttribute("faceDente") String faceDente,
			@ModelAttribute("patologias") List<Integer> patologiasId, @ModelAttribute("local") String local,
			@ModelAttribute("idOdontograma") Integer idOdontograma, @ModelAttribute("descricao") String descricao,
			Authentication auth) throws ProntuarioException {

		Usuario usuario = (Usuario) auth.getPrincipal();
		
		Aluno aluno = alunoService.buscarPorMatricula(usuario.getMatricula());

		Map<String, Object> map = new HashMap<String, Object>();
		List<Patologia> patologias = patologiaService.salvar(faceDente, patologiasId, local, idOdontograma, descricao,
				aluno);

		map.put("patologias", patologias);
		return map;
	}
	
	@PreAuthorize(PERMISSAO_ESTUDANTE)
	@PostMapping("/adicionarProcedimento")
	public @ResponseBody Map<String, Object> adicionarProcedimento(@ModelAttribute("faceDente") String faceDente,
			@ModelAttribute("procedimentos") List<Integer> procedimentosId, @ModelAttribute("local") String local,
			@ModelAttribute("idOdontograma") Integer idOdontograma, @ModelAttribute("descricao") String descricao,
			@ModelAttribute("preExistente") Boolean preExistente, Authentication auth,
			@RequestParam(required = false, value = "patologias") List<Integer> patologias,
			@ModelAttribute("data") @DateTimeFormat(pattern = DATE_PATTERN) Date data) throws ProntuarioException {

		Usuario usuario = (Usuario) auth.getPrincipal();
		
		Aluno aluno = alunoService.buscarPorMatricula(usuario.getMatricula());

		Map<String, Object> map = new HashMap<String, Object>();
		List<Procedimento> procedimentos = procedimentoService.salvar(faceDente, procedimentosId, local, idOdontograma,
				descricao, aluno, preExistente, patologias, data);

		map.put("procedimentos", procedimentos);
		map.put("patologiasTratadas", patologiaService.buscarPatologiasTratadas(odontogramaService.buscarPorId(idOdontograma)));
		return map;
	}

	@GetMapping("/buscarPatologias/{id}")
	public @ResponseBody Map<String, Object> buscarPatologias(@PathVariable("id") Odontograma odontograma,
			Authentication auth) {
		Map<String, Object> map = new HashMap<String, Object>();

		Usuario usuario = (Usuario) auth.getPrincipal();
		map.put("patologiasOdontograma", patologiaService.buscarPatologiasOdontograma(odontograma, usuario));
		return map;
	}

	@GetMapping("/buscarProcedimentos/{id}")
	public @ResponseBody Map<String, Object> buscarProcedimentos(@PathVariable("id") Odontograma odontograma,
			Authentication auth) {
		Map<String, Object> map = new HashMap<String, Object>();

		Usuario usuario = (Usuario) auth.getPrincipal();
		map.put("procedimentosOdontograma",
				procedimentoService.buscarProcedimentosOdontograma(odontograma, usuario.getId()));
		return map;
	}

	@GetMapping("/buscarProcedimentosExistentes/{id}")
	public @ResponseBody Map<String, Object> buscarProcedimentosExistentes(@PathVariable("id") Odontograma odontograma,
			Authentication auth) {
		Map<String, Object> map = new HashMap<String, Object>();

		Usuario usuario = (Usuario) auth.getPrincipal();
		map.put("procedimentosExistentesOdontograma",
				procedimentoService.buscarProcedimentosExistentesOdontograma(odontograma, usuario.getId()));
		return map;
	}

	@GetMapping("/patologias/{idPatologia}")
	public ModelAndView buscarPatologia(@PathVariable("idPatologia") Patologia patologia) {
		ModelAndView modelAndView = new ModelAndView(FRAGMENT_MODAL_DETALHES_PATOLOGIA);
		modelAndView.addObject("patologia", patologia);
		return modelAndView;
	}

	@PreAuthorize(PERMISSAO_ESTUDANTE)
	@PostMapping("/tratar/{idPatologia}")
	public ResponseEntity<?> tratarPatologia(@PathVariable("idPatologia") Patologia patologia,
			@ModelAttribute Tratamento tratamento, Authentication auth) {
		DefinirResponsavelTratamento(tratamento, auth, patologia);
		patologiaService.tratar(patologia);
		ResponseEntity<List<Patologia>> responseEntity = new ResponseEntity<List<Patologia>>(
				patologiaService.buscarPatologiasTratadas(patologia.getOdontograma()), HttpStatus.OK);
		return responseEntity;
	}
	
	public void DefinirResponsavelTratamento(Tratamento tratamento, Authentication auth, Patologia patologia) {
		tratamento.setResponsavel((Aluno) auth.getPrincipal());
		patologia.setTratamento(tratamento);
	}

	
	@GetMapping("/buscar-patologia")
	public @ResponseBody List<TipoPatologia> buscaPatologia(@RequestParam("query") String query) {

		List<TipoPatologia> resultado = tipoPatologiaService.buscarPorNome(query);

		return resultado;
	}

	@GetMapping("/buscar-procedimento")
	public @ResponseBody List<TipoProcedimento> buscaProcedimento(@RequestParam("query") String query) {

		List<TipoProcedimento> resultado = tipoProcedimentoService.buscarPorNome(query);

		return resultado;
	}

	@GetMapping("/buscar-patologia/{odontograma}")
	public @ResponseBody List<Patologia> buscaPatologiasProcedimento(
			@PathVariable("odontograma") Odontograma odontograma,
			@RequestParam(value = "face", required = false) String faceDente,
			@RequestParam(value = "dente", required = false) String dente, Authentication auth) {

		Aluno aluno = (Aluno) auth.getPrincipal();
		List<Patologia> patologias = patologiaService.buscarPatologiasDentePaciente(odontograma, faceDente, dente, aluno);

		return patologias;
	}

}