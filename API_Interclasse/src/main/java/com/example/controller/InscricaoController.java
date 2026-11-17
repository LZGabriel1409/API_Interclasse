package com.example.controller;

import com.example.dao.InscricaoDao;
import com.example.dao.ModalidadeDao;
import com.example.dao.TimeDao;
import com.example.model.Inscricao;
import com.example.model.Modalidade;
import com.example.util.JsonUtil;
import com.example.util.SessaoUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller de Inscricao (Atividade 6).
 *
 * Rotas:
 *   GET  /api/inscricoes                -> lista todas as inscricoes
 *   GET  /api/inscricoes?modalidadeId=1 -> lista as inscricoes da modalidade
 *   GET  /api/inscricoes?id=1           -> busca uma inscricao
 *   POST /api/inscricoes                -> inscreve um time (nasce PENDENTE)
 *   PUT  /api/inscricoes?id=1           -> organizador confirma ou nega
 *
 * Regras: um time tem no maximo uma inscricao, e o status inicial e sempre
 * PENDENTE; somente ORGANIZADOR altera o status para CONFIRMADA ou NEGADA.
 */
@WebServlet("/inscricoes")
public class InscricaoController extends HttpServlet {

    private final InscricaoDao dao = new InscricaoDao();
    private final TimeDao timeDao = new TimeDao();
    private final ModalidadeDao modalidadeDao = new ModalidadeDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (semLogin(request, response)) {
            return;
        }

        try {
            String id = request.getParameter("id");
            if (id != null) {
                Inscricao inscricao = dao.buscarPorId(Integer.parseInt(id));
                if (inscricao == null) {
                    JsonUtil.enviarErro(response, HttpServletResponse.SC_NOT_FOUND, "Inscricao nao encontrada");
                    return;
                }
                JsonUtil.enviar(response, inscricao);
                return;
            }

            String modalidadeId = request.getParameter("modalidadeId");
            List<Inscricao> inscricoes = (modalidadeId == null)
                    ? dao.listar()
                    : dao.listarPorModalidade(Integer.parseInt(modalidadeId));
            JsonUtil.enviar(response, inscricoes);

        } catch (NumberFormatException e) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST, "id ou modalidadeId invalido");
        } catch (SQLException e) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (semLogin(request, response)) {
            return;
        }

        try {
            Inscricao inscricao = JsonUtil.ler(request, Inscricao.class);
            if (inscricao == null || inscricao.getTimeId() == null || inscricao.getModalidadeId() == null) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST,
                        "timeId e modalidadeId sao obrigatorios");
                return;
            }

            if (timeDao.buscarPorId(inscricao.getTimeId()) == null) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_NOT_FOUND, "Time nao encontrado");
                return;
            }

            Modalidade modalidade = modalidadeDao.buscarPorId(inscricao.getModalidadeId());
            if (modalidade == null) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_NOT_FOUND, "Modalidade nao encontrada");
                return;
            }
            if (modalidade.getAtivo() != null && modalidade.getAtivo() == 0) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_CONFLICT, "modalidade inativa");
                return;
            }

            // A tabela aceita uma unica inscricao por time, o que ja cobre a
            // regra de um time por modalidade e a inscricao duplicada.
            if (dao.buscarPorTime(inscricao.getTimeId()) != null) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_CONFLICT,
                        "este time ja possui uma inscricao");
                return;
            }

            // A inscricao nasce sempre PENDENTE, com a data de hoje.
            inscricao.setStatus("PENDENTE");
            inscricao.setDataInscricao(LocalDate.now().toString());

            dao.inserir(inscricao);
            JsonUtil.enviar(response, inscricao);

        } catch (SQLException e) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /** Somente organizador pode confirmar (CONFIRMADA) ou negar (NEGADA) uma inscricao. */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (semLogin(request, response) || semPermissao(request, response)) {
            return;
        }

        try {
            String id = request.getParameter("id");
            if (id == null) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST, "id e obrigatorio");
                return;
            }

            Inscricao dados = JsonUtil.ler(request, Inscricao.class);
            String status = (dados == null || dados.getStatus() == null) ? null : dados.getStatus().toUpperCase();
            if (!"CONFIRMADA".equals(status) && !"NEGADA".equals(status)) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST,
                        "status deve ser CONFIRMADA ou NEGADA");
                return;
            }

            Inscricao inscricao = dao.buscarPorId(Integer.parseInt(id));
            if (inscricao == null) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_NOT_FOUND, "Inscricao nao encontrada");
                return;
            }

            dao.atualizarStatus(inscricao.getId(), status);
            inscricao.setStatus(status);
            JsonUtil.enviar(response, inscricao);

        } catch (NumberFormatException e) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST, "id invalido");
        } catch (SQLException e) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /** Responde 401 e devolve true quando nao ha usuario logado. */
    private boolean semLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (SessaoUtil.estaLogado(request)) {
            return false;
        }
        JsonUtil.enviarErro(response, HttpServletResponse.SC_UNAUTHORIZED, "faca login para continuar");
        return true;
    }

    /** Responde 403 e devolve true quando o usuario logado nao e organizador. */
    private boolean semPermissao(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (SessaoUtil.ehOrganizador(request)) {
            return false;
        }
        JsonUtil.enviarErro(response, HttpServletResponse.SC_FORBIDDEN, "acesso restrito a organizadores");
        return true;
    }
}
