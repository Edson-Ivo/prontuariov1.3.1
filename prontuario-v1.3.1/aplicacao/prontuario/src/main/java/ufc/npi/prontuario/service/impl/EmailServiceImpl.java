package ufc.npi.prontuario.service.impl;

import static ufc.npi.prontuario.util.EmailConstants.ASSUNTO_ATENDIMENTO_ANDAMENTO;
import static ufc.npi.prontuario.util.EmailConstants.ASSUNTO_ATENDIMENTO_AVALIACAO;
import static ufc.npi.prontuario.util.EmailConstants.ASSUNTO_RECUPERACAO_SENHA;
import static ufc.npi.prontuario.util.EmailConstants.DATA;
import static ufc.npi.prontuario.util.EmailConstants.DISCIPLINA;
import static ufc.npi.prontuario.util.EmailConstants.EMAIL_REMETENTE;
import static ufc.npi.prontuario.util.EmailConstants.RESPONSAVEL;
import static ufc.npi.prontuario.util.EmailConstants.TEMPLATE_EMAIL_RECUPERACAO_SENHA;
import static ufc.npi.prontuario.util.EmailConstants.TEXTO_ATENDIMENTO_ANDAMENTO;
import static ufc.npi.prontuario.util.EmailConstants.TEXTO_ATENDIMENTO_AVALIACAO;
import static ufc.npi.prontuario.util.EmailConstants.TURMA;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import ufc.npi.prontuario.model.Atendimento;
import ufc.npi.prontuario.model.Atendimento.Status;
import ufc.npi.prontuario.model.Token;
import ufc.npi.prontuario.repository.AtendimentoRepository;
import ufc.npi.prontuario.service.EmailService;

@Service
public class EmailServiceImpl implements EmailService {

	@Autowired
	private Environment env;

	@Autowired
	private JavaMailSender mailSender;

	@Autowired
	private TemplateEngine templateEngine;

	@Autowired
	private AtendimentoRepository atendimentoRepository;

	private DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.FULL);

	private void enviarEmail(MimeMessage email) {
		this.mailSender.send(email);
	}

	private void enviarEmails(List<MimeMessage> emails) {
		for (MimeMessage email : emails) {
			this.enviarEmail(email);
		}
	}

	private String formatDate(Date date) {
		return dateFormat.format(date);
	}

	@Override
	public void emailRecuperacaoSenha(Token token) {
		Runnable email = new Runnable() {

			@Override
			public void run() {
				final MimeMessage mimeMessage = mailSender.createMimeMessage();
				final MimeMessageHelper mensagem = new MimeMessageHelper(mimeMessage, "UTF-8");

				final Context contexto = new Context();

				contexto.setVariable("token", token.getToken());
				contexto.setVariable("link", env.getProperty("sistema.link"));

				final String conteudo = templateEngine.process(TEMPLATE_EMAIL_RECUPERACAO_SENHA, contexto);

				try {
					mensagem.setTo(token.getUsuario().getEmail());
					mensagem.setFrom(EMAIL_REMETENTE);
					mensagem.setSubject(ASSUNTO_RECUPERACAO_SENHA);
					mensagem.setText(conteudo, true);
				} catch (MessagingException e) {
					e.printStackTrace();
				}

				enviarEmail(mimeMessage);
			}
		};

		Thread thread = new Thread(email);
		thread.start();
	}

	@Override
	public void notificarAtendimentoAndamento() {
		List<MimeMessage> emails = new ArrayList<>();
		List<Atendimento> atendimentos = atendimentoRepository.findAllByStatus(Status.EM_ANDAMENTO);

		if (!atendimentos.isEmpty()) {
			MimeMessage mimeMessage;
			MimeMessageHelper mensagem;
			String destinatario = null;
			String assuntoEmail = null;
			String texto = null;
			for (Atendimento a : atendimentos) {
				mimeMessage = mailSender.createMimeMessage();
				mensagem = new MimeMessageHelper(mimeMessage, "UTF-8");

				destinatario = a.getResponsavel().getEmail();
				assuntoEmail = ASSUNTO_ATENDIMENTO_ANDAMENTO;
				texto = TEXTO_ATENDIMENTO_ANDAMENTO.replace(DATA, formatDate(a.getData()))
						.replaceAll(TURMA, a.getTurma().getNome())
						.replaceAll(DISCIPLINA, a.getTurma().getDisciplina().getNome());

				try {
					mensagem.setTo(destinatario);
					mensagem.setFrom(EMAIL_REMETENTE);
					mensagem.setSubject(assuntoEmail);
					mensagem.setText(texto, true);
					emails.add(mimeMessage);
				} catch (MessagingException e) {
					e.printStackTrace();
					continue;
				}
			}
		}

		enviarEmails(emails);
	}

	@Override
	public void notificarAtendimentoAvaliacao() {
		List<MimeMessage> emails = new ArrayList<>();
		List<Atendimento> atendimentos = atendimentoRepository.findAllByStatus(Status.REALIZADO);

		if (!atendimentos.isEmpty()) {
			for (Atendimento atendimento : atendimentos) {
				try {
					MimeMessage mimeMessage = mailSender.createMimeMessage();

					criarMensagemAtendimento(mimeMessage, atendimento);

					emails.add(mimeMessage);
				} catch (MessagingException e) {
					e.printStackTrace();
					continue;
				}
			}
		}

		enviarEmails(emails);
	}

	private void criarMensagemAtendimento(MimeMessage mimeMessage, Atendimento atendimento) throws MessagingException {
		MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, "UTF-8");

		String destinatario = atendimento.getProfessor().getEmail();
		String assunto = ASSUNTO_ATENDIMENTO_AVALIACAO;
		String texto = TEXTO_ATENDIMENTO_AVALIACAO.replace(DATA, formatDate(atendimento.getData()))
				.replaceAll(TURMA, atendimento.getTurma().getNome())
				.replaceAll(DISCIPLINA, atendimento.getTurma().getDisciplina().getNome())
				.replaceAll(RESPONSAVEL, atendimento.getResponsavel().getNome());

		mimeMessageHelper.setTo(destinatario);
		mimeMessageHelper.setFrom(EMAIL_REMETENTE);
		mimeMessageHelper.setSubject(assunto);
		mimeMessageHelper.setText(texto, true);
	}
}
