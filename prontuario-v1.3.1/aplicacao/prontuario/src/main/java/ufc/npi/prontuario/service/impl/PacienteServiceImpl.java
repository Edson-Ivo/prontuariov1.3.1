package ufc.npi.prontuario.service.impl;

import static ufc.npi.prontuario.util.ExceptionSuccessConstants.ERRO_PACIENTE_CPF_EXISTENTE;
import static ufc.npi.prontuario.util.ExceptionSuccessConstants.ERRO_PACIENTE_CNS_EXISTENTE;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ufc.npi.prontuario.exception.ProntuarioException;
import ufc.npi.prontuario.model.Odontograma;
import ufc.npi.prontuario.model.Paciente;
import ufc.npi.prontuario.model.PacienteAnamnese;
import ufc.npi.prontuario.repository.PacienteRepository;
import ufc.npi.prontuario.service.PacienteService;

@Service
public class PacienteServiceImpl implements PacienteService {

    @Autowired
    private PacienteRepository pacienteRepository;

    @Override
    public void salvar(Paciente paciente) throws ProntuarioException {
        if (paciente.getId() == null) {
            paciente.setOdontograma(new Odontograma());
        }

        if (!paciente.getCpf().isEmpty()) {
            Paciente pacienteExistente = buscarByCpf(paciente.getCpf());

            if (pacienteExistente != null && !pacienteExistente.equals(paciente)) {
                throw new ProntuarioException(ERRO_PACIENTE_CPF_EXISTENTE);
            }
        }

        if (!paciente.getCns().isEmpty()) {
            Paciente pacienteExistente = buscarByCns(paciente.getCns());

            if (pacienteExistente != null && !pacienteExistente.equals(paciente)) {
                throw new ProntuarioException(ERRO_PACIENTE_CNS_EXISTENTE);
            }
        }

        pacienteRepository.save(paciente);
    }

    @Override
    public List<Paciente> buscarTudo() {
        return pacienteRepository.findAll();
    }

    @Override
    public void adicionarAnamnese(Paciente paciente, PacienteAnamnese anamnese) {
        paciente.addPacienteAnamnese(anamnese);
        pacienteRepository.save(paciente);
    }

    @Override
    public Paciente buscarByCpf(String cpf) {

        return pacienteRepository.findByCpf(cpf);
    }

    @Override
    public Paciente buscarByCns(String cns) {
        return pacienteRepository.findByCns(cns);
    }
}
