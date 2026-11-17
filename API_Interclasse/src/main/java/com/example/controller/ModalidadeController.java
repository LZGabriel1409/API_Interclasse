package com.example.controller;

import com.example.dao.ModalidadeDao;
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

/**
 * Controller de Modalidade (Atividade 5).
 *
 * Rotas:
 *   GET    /api/modalidades              -> lista todas as modalidades
 *   GET    /api/modalidades?id=1         -> busca uma modalidade
 *   POST   /api/modalidades              -> cadastra uma modalidade
 *   PUT    /api/modalidades?id=1         -> atualiza o nome e/ou o ativo
 *   PUT    /api/modalidades?id=1&ativo=0 -> ativa (1) ou desativa (0) sem enviar JSON
 *   DELETE /api/modalidades?id=1         -> remove uma modalidade
 *
 * Todas exigem usuario logado (participante ou organizador).
 */
@WebServlet("/modalidades")
public class ModalidadeController extends HttpServlet {

    private final ModalidadeDao dao = new ModalidadeDao();

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

            Modalidade modalidade = dao.buscarPorId(Integer.parseInt(id));
            if (modalidade == null) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_NOT_FOUND, "Modalidade nao encontrada");
                return;
            }
            JsonUtil.enviar(response, modalidade);

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
            Modalidade modalidade = JsonUtil.ler(request, Modalidade.class);
            if (modalidade == null || modalidade.getNome() == null || modalidade.getNome().isBlank()) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST, "nome e obrigatorio");
                return;
            }
            if (modalidade.getAtivo() == null) {
                modalidade.setAtivo(1);
            }
            if (modalidade.getAtivo() != 0 && modalidade.getAtivo() != 1) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST, "ativo deve ser 0 ou 1");
                return;
            }
            if (dao.buscarPorNome(modalidade.getNome()) != null) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_CONFLICT, "modalidade ja cadastrada");
                return;
            }

            dao.inserir(modalidade);
            JsonUtil.enviar(response, modalidade);

        } catch (SQLException e) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Atualiza a modalidade. Os campos que nao vierem no corpo (ou ?ativo=) mantem
     * o valor atual, entao o mesmo PUT serve para renomear e para ativar/desativar.
     */
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

            Modalidade modalidade = dao.buscarPorId(Integer.parseInt(id));
            if (modalidade == null) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_NOT_FOUND, "Modalidade nao encontrada");
                return;
            }

            Modalidade dados = JsonUtil.ler(request, Modalidade.class);
            String ativoParam = request.getParameter("ativo");
            if (dados == null && ativoParam == null) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST,
                        "informe o nome ou o ativo no corpo da requisicao, ou o parametro ativo na URL");
                return;
            }

            if (dados != null && dados.getNome() != null && !dados.getNome().isBlank()) {
                boolean mudouNome = !dados.getNome().equals(modalidade.getNome());
                if (mudouNome && dao.buscarPorNome(dados.getNome()) != null) {
                    JsonUtil.enviarErro(response, HttpServletResponse.SC_CONFLICT,
                            "nome de modalidade ja cadastrado");
                    return;
                }
                modalidade.setNome(dados.getNome());
            }

            int ativo;
            if (dados != null && dados.getAtivo() != null) {
                ativo = dados.getAtivo();
            } else if (ativoParam != null) {
                ativo = Integer.parseInt(ativoParam);
            } else {
                ativo = modalidade.getAtivo();
            }
            if (ativo != 0 && ativo != 1) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST, "ativo deve ser 0 ou 1");
                return;
            }
            modalidade.setAtivo(ativo);

            dao.atualizar(modalidade);
            JsonUtil.enviar(response, modalidade);

        } catch (NumberFormatException e) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST, "id ou ativo invalido");
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
                JsonUtil.enviarErro(response, HttpServletResponse.SC_NOT_FOUND, "Modalidade nao encontrada");
                return;
            }
            JsonUtil.enviarMensagem(response, HttpServletResponse.SC_OK, "Modalidade removida");

        } catch (NumberFormatException e) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST, "id invalido");
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().toUpperCase().contains("FOREIGN KEY")) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_CONFLICT,
                        "modalidade possui inscricoes ou partidas e nao pode ser removida");
                return;
            }
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

    private boolean semPermissao(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (SessaoUtil.ehOrganizador(request)) return false;
        JsonUtil.enviarErro(response, SessaoUtil.estaLogado(request) ? HttpServletResponse.SC_FORBIDDEN : HttpServletResponse.SC_UNAUTHORIZED,
                SessaoUtil.estaLogado(request) ? "acesso restrito a organizadores" : "faca login para continuar");
        return true;
    }
}
