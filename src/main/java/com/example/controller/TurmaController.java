package com.example.controller;

import com.example.dao.TurmaDao;
import com.example.model.Turma;
import com.example.util.JsonUtil;
import com.example.util.SessaoUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Controller de Turma (Atividade 4).
 *
 * Rotas:
 *   GET    /api/turmas      -> lista todas as turmas
 *   GET    /api/turmas?id=1 -> busca uma turma
 *   POST   /api/turmas      -> cadastra uma turma
 *   PUT    /api/turmas?id=1 -> atualiza uma turma
 *   DELETE /api/turmas?id=1 -> remove uma turma
 *
 * Todas exigem usuario logado (participante ou organizador).
 */
@WebServlet("/turmas")
public class TurmaController extends HttpServlet {

    private final TurmaDao dao = new TurmaDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (semLogin(request, response)) {
            return;
        }

        try {
            String id = request.getParameter("id");
            if (id == null) {
                JsonUtil.enviar(response, dao.listar());
                return;
            }

            Turma turma = dao.buscarPorId(Integer.parseInt(id));
            if (turma == null) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_NOT_FOUND, "Turma nao encontrada");
                return;
            }
            JsonUtil.enviar(response, turma);

        } catch (NumberFormatException e) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST, "id invalido");
        } catch (SQLException e) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (semLogin(request, response) || semPermissao(request, response)) {
            return;
        }

        try {
            Turma turma = JsonUtil.ler(request, Turma.class);
            String erro = validar(turma);
            if (erro != null) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST, erro);
                return;
            }

            dao.inserir(turma);
            JsonUtil.enviar(response, turma);

        } catch (SQLException e) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

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

            Turma turma = JsonUtil.ler(request, Turma.class);
            String erro = validar(turma);
            if (erro != null) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST, erro);
                return;
            }
            turma.setId(Integer.parseInt(id));

            if (!dao.atualizar(turma)) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_NOT_FOUND, "Turma nao encontrada");
                return;
            }
            JsonUtil.enviar(response, turma);

        } catch (NumberFormatException e) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST, "id invalido");
        } catch (SQLException e) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (semLogin(request, response) || semPermissao(request, response)) {
            return;
        }

        try {
            String id = request.getParameter("id");
            if (id == null) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST, "id e obrigatorio");
                return;
            }

            if (!dao.deletar(Integer.parseInt(id))) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_NOT_FOUND, "Turma nao encontrada");
                return;
            }
            JsonUtil.enviarMensagem(response, HttpServletResponse.SC_OK, "Turma removida");

        } catch (NumberFormatException e) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST, "id invalido");
        } catch (SQLException e) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /** Valida os campos obrigatorios. Retorna a mensagem de erro ou null quando esta tudo certo. */
    private String validar(Turma turma) {
        if (turma == null) {
            return "corpo da requisicao invalido";
        }
        if (turma.getNome() == null || turma.getNome().isBlank()) {
            return "nome e obrigatorio";
        }
        if (turma.getCurso() == null || turma.getCurso().isBlank()) {
            return "curso e obrigatorio";
        }
        if (turma.getAno() == null || turma.getAno() <= 0) {
            return "ano e obrigatorio e deve ser maior que zero";
        }
        return null;
    }

    /** Responde 401 e devolve true quando nao ha usuario logado. */
    private boolean semLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (SessaoUtil.estaLogado(request)) {
            return false;
        }
        JsonUtil.enviarErro(response, HttpServletResponse.SC_UNAUTHORIZED, "faca login para continuar");
        return true;
    }

    private boolean semPermissao(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (SessaoUtil.ehOrganizador(request)) return false;
        JsonUtil.enviarErro(response, SessaoUtil.estaLogado(request) ? HttpServletResponse.SC_FORBIDDEN : HttpServletResponse.SC_UNAUTHORIZED,
                SessaoUtil.estaLogado(request) ? "acesso restrito a organizadores" : "faca login para continuar");
        return true;
    }
}
