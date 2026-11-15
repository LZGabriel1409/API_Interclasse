package com.example.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.example.dao.ConfiguracaoDao;
import com.example.dao.InscricaoDao;
import com.example.dao.ModalidadeDao;
import com.example.dao.PartidaDao;
import com.example.dao.TimeDao;
import com.example.model.Partida;
import com.example.util.JsonUtil;
import com.example.util.SessaoUtil;

/**
 * Controller de Partida (Atividade 7 e Fase 3).
 *
 * Rotas:
 *   GET     /api/partidas                                     -> lista partidas
 *   GET     /api/partidas?modalidadeId=1&status=AGENDADA&data=2026-11-20 -> filtra
 *   GET     /api/partidas?id=1                                -> busca uma partida
 *   POST    /api/partidas                                     -> agenda uma partida
 *   PUT     /api/partidas?id=1                                -> atualiza o placar (organizador)
 *   PATCH   /api/partidas?id=1                                -> atualiza o placar (organizador)
 *   POST    /api/partidas/finalizar?id=1                      -> finaliza a partida (organizador)
 *
 * Qualquer outra sub-rota de /api/partidas/* responde 404.
 */
@WebServlet("/partidas/*")
public class PartidaController extends HttpServlet {

    private static final List<String> STATUS = Arrays.asList(
            "AGENDADA", "EM_ANDAMENTO", "FINALIZADA", "CANCELADA");
    private static final List<String> FASES = Arrays.asList(
            "AVULSO", "GRUPOS", "QUARTAS", "SEMI", "FINAL");

    private final PartidaDao dao = new PartidaDao();
    private final InscricaoDao inscricaoDao = new InscricaoDao();
    private final ModalidadeDao modalidadeDao = new ModalidadeDao();
    private final TimeDao timeDao = new TimeDao();
    private final ConfiguracaoDao configuracaoDao = new ConfiguracaoDao();

    /** A Servlet API 3.1 nao expoe doPatch, entao o PATCH e encaminhado para o doPut. */
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if ("PATCH".equalsIgnoreCase(request.getMethod())) {
            doPut(request, response);
            return;
        }
        super.service(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (rotaDesconhecida(request, response) || semLogin(request, response)) {
            return;
        }

        try {
            String id = request.getParameter("id");
            if (id != null) {
                Partida partida = dao.buscarPorId(Integer.parseInt(id));
                if (partida == null) {
                    JsonUtil.enviarErro(response, HttpServletResponse.SC_NOT_FOUND, "Partida nao encontrada");
                    return;
                }
                JsonUtil.enviar(response, partida);
                return;
            }

            String status = normalizar(request.getParameter("status"));
            if (status != null && !STATUS.contains(status)) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST,
                        "status deve ser AGENDADA, EM_ANDAMENTO, FINALIZADA ou CANCELADA");
                return;
            }

            String modalidadeParam = request.getParameter("modalidadeId");
            Integer modalidadeId = (modalidadeParam == null) ? null : Integer.parseInt(modalidadeParam);
            JsonUtil.enviar(response, dao.listarComFiltros(modalidadeId, status, request.getParameter("data")));

        } catch (NumberFormatException e) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST, "id ou modalidadeId invalido");
        } catch (SQLException e) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String rota = subRota(request);
        if ("/finalizar".equals(rota)) {
            finalizar(request, response);
            return;
        }
        if (rota != null) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_NOT_FOUND, "rota nao encontrada");
            return;
        }
        agendar(request, response);
    }

    /** Agenda uma partida entre dois times confirmados na mesma modalidade. */
    private void agendar(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (semLogin(request, response) || semPermissao(request, response)) {
            return;
        }

        try {
            Partida partida = JsonUtil.ler(request, Partida.class);
            if (partida == null) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST, "corpo da requisicao invalido");
                return;
            }
            if (partida.getModalidadeId() == null || partida.getTimeAId() == null || partida.getTimeBId() == null) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST,
                        "modalidadeId, timeAId e timeBId sao obrigatorios");
                return;
            }
            if (partida.getData() == null || partida.getData().isBlank()
                    || partida.getHora() == null || partida.getHora().isBlank()
                    || partida.getLocal() == null || partida.getLocal().isBlank()) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST,
                        "data, hora e local sao obrigatorios");
                return;
            }
            if (partida.getTimeAId().equals(partida.getTimeBId())) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST,
                        "os times da partida devem ser diferentes");
                return;
            }
            if (modalidadeDao.buscarPorId(partida.getModalidadeId()) == null) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_NOT_FOUND, "Modalidade nao encontrada");
                return;
            }
            if (timeDao.buscarPorId(partida.getTimeAId()) == null
                    || timeDao.buscarPorId(partida.getTimeBId()) == null) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_NOT_FOUND, "Time nao encontrado");
                return;
            }
            if (!inscricaoDao.temInscricaoConfirmada(partida.getTimeAId(), partida.getModalidadeId())
                    || !inscricaoDao.temInscricaoConfirmada(partida.getTimeBId(), partida.getModalidadeId())) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_CONFLICT,
                        "os dois times precisam ter inscricao CONFIRMADA nesta modalidade");
                return;
            }

            String fase = normalizar(partida.getFase());
            if (fase == null) {
                fase = "AVULSO";
            } else if (!FASES.contains(fase)) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST,
                        "fase deve ser AVULSO, GRUPOS, QUARTAS, SEMI ou FINAL");
                return;
            }

            // Toda partida nasce agendada e sem placar.
            partida.setFase(fase);
            partida.setStatus("AGENDADA");
            partida.setPlacarA(null);
            partida.setPlacarB(null);

            dao.inserir(partida);
            JsonUtil.enviar(response, partida);

        } catch (SQLException e) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /** Atualiza o placar (e opcionalmente o status) enquanto a partida nao esta finalizada. */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (rotaDesconhecida(request, response) || semLogin(request, response) || semPermissao(request, response)) {
            return;
        }

        try {
            String id = request.getParameter("id");
            if (id == null) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST, "id e obrigatorio");
                return;
            }

            Partida dados = JsonUtil.ler(request, Partida.class);
            if (dados == null || dados.getPlacarA() == null || dados.getPlacarB() == null) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST,
                        "placarA e placarB sao obrigatorios");
                return;
            }
            if (dados.getPlacarA() < 0 || dados.getPlacarB() < 0) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST,
                        "placar nao pode ser negativo");
                return;
            }

            Partida partida = dao.buscarPorId(Integer.parseInt(id));
            if (partida == null) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_NOT_FOUND, "Partida nao encontrada");
                return;
            }
            if ("FINALIZADA".equals(partida.getStatus())) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_CONFLICT,
                        "partida finalizada nao pode ter o placar alterado");
                return;
            }

            String status = normalizar(dados.getStatus());
            if (status != null && (!STATUS.contains(status) || "FINALIZADA".equals(status))) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST,
                        "status deve ser AGENDADA, EM_ANDAMENTO ou CANCELADA; use /partidas/finalizar para finalizar");
                return;
            }

            dao.atualizarPlacar(partida.getId(), dados.getPlacarA(), dados.getPlacarB());
            partida.setPlacarA(dados.getPlacarA());
            partida.setPlacarB(dados.getPlacarB());

            if (status != null) {
                dao.atualizarStatus(partida.getId(), status);
                partida.setStatus(status);
            }

            JsonUtil.enviar(response, partida);

        } catch (NumberFormatException e) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST, "id invalido");
        } catch (SQLException e) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /** Finaliza a partida (somente organizador), aplicando a regra de permissao de empate. */
    private void finalizar(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (semLogin(request, response) || semPermissao(request, response)) {
            return;
        }

        try {
            String id = request.getParameter("id");
            if (id == null) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST, "id e obrigatorio");
                return;
            }

            Partida partida = dao.buscarPorId(Integer.parseInt(id));
            if (partida == null) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_NOT_FOUND, "Partida nao encontrada");
                return;
            }
            if ("FINALIZADA".equals(partida.getStatus())) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_CONFLICT, "partida ja finalizada");
                return;
            }
            if (partida.getPlacarA() == null || partida.getPlacarB() == null) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST,
                        "informe o placar antes de finalizar a partida");
                return;
            }

            // Atividade 10 — Validação da regra PERMITE_EMPATE
            String permiteEmpate = configuracaoDao.obterValor("PERMITE_EMPATE", "1");
            if ("0".equals(permiteEmpate) && partida.getPlacarA().equals(partida.getPlacarB())) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Empates nao sao permitidos pelas regras atuais do campeonato.");
                return;
            }

            dao.atualizarStatus(partida.getId(), "FINALIZADA");
            partida.setStatus("FINALIZADA");
            JsonUtil.enviar(response, partida);

        } catch (NumberFormatException e) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST, "id invalido");
        } catch (SQLException e) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /** Parte da URL depois de /partidas (ex: "/finalizar") ou null na rota raiz. */
    private String subRota(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path == null || path.isBlank() || "/".equals(path)) {
            return null;
        }
        return path.toLowerCase();
    }

    /** Responde 404 e devolve true para sub-rotas que nao existem. */
    private boolean rotaDesconhecida(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (subRota(request) == null) {
            return false;
        }
        JsonUtil.enviarErro(response, HttpServletResponse.SC_NOT_FOUND, "rota nao encontrada");
        return true;
    }

    /** Texto em maiusculas, tratando parametros ausentes ou vazios como null. */
    private String normalizar(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.toUpperCase();
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