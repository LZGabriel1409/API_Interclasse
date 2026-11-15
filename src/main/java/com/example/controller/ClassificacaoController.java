package com.example.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.example.dao.ConfiguracaoDao;
import com.example.dao.PartidaDao;
import com.example.dao.ModalidadeDao;
import com.example.dao.TimeDao;
import com.example.model.Partida;
import com.example.model.Time;
import com.example.util.JsonUtil;
import com.example.util.SessaoUtil;

@WebServlet("/classificacao")
public class ClassificacaoController extends HttpServlet {

    private final TimeDao timeDao = new TimeDao();
    private final ModalidadeDao modalidadeDao = new ModalidadeDao();
    private final PartidaDao partidaDao = new PartidaDao();
    private final ConfiguracaoDao configuracaoDao = new ConfiguracaoDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!SessaoUtil.estaLogado(req)) {
            JsonUtil.enviarErro(resp, HttpServletResponse.SC_UNAUTHORIZED, "faca login para consultar a classificacao");
            return;
        }
        String modalidadeIdStr = req.getParameter("modalidadeId");

        if (modalidadeIdStr == null || modalidadeIdStr.isEmpty()) {
            JsonUtil.enviarErro(resp, HttpServletResponse.SC_BAD_REQUEST, "O parâmetro modalidadeId é obrigatório.");
            return;
        }

        try {
            int modalidadeId = Integer.parseInt(modalidadeIdStr);
            if (modalidadeDao.buscarPorId(modalidadeId) == null) {
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_NOT_FOUND, "modalidade nao encontrada");
                return;
            }

            int ptsVitoria = Integer.parseInt(configuracaoDao.obterValor("PONTOS_VITORIA", "3"));
            int ptsEmpate = Integer.parseInt(configuracaoDao.obterValor("PONTOS_EMPATE", "1"));
            int ptsDerrota = Integer.parseInt(configuracaoDao.obterValor("PONTOS_DERROTA", "0"));

            List<Time> times = timeDao.listar();
            List<Partida> partidas = partidaDao.listarComFiltros(modalidadeId, null, null);

            Map<Integer, EstatisticaTime> tabela = new HashMap<>();
            for (Time t : times) {
                tabela.put(t.getId(), new EstatisticaTime(t.getId(), t.getNome()));
            }

            for (Partida p : partidas) {
                if ("FINALIZADA".equalsIgnoreCase(p.getStatus())) {
                    Integer timeAId = p.getTimeAId();
                    Integer timeBId = p.getTimeBId();
                    Integer placarA = p.getPlacarA();
                    Integer placarB = p.getPlacarB();

                    if (timeAId != null && timeBId != null && placarA != null && placarB != null) {
                        EstatisticaTime t1 = tabela.get(timeAId);
                        EstatisticaTime t2 = tabela.get(timeBId);

                        if (t1 != null && t2 != null) {
                            t1.golsPro += placarA;
                            t1.golsContra += placarB;
                            t2.golsPro += placarB;
                            t2.golsContra += placarA;

                            if (placarA > placarB) {
                                t1.vitorias++;
                                t1.pontos += ptsVitoria;
                                t2.derrotas++;
                                t2.pontos += ptsDerrota;
                            } else if (placarA < placarB) {
                                t2.vitorias++;
                                t2.pontos += ptsVitoria;
                                t1.derrotas++;
                                t1.pontos += ptsDerrota;
                            } else {
                                t1.empates++;
                                t1.pontos += ptsEmpate;
                                t2.empates++;
                                t2.pontos += ptsEmpate;
                            }
                        }
                    }
                }
            }

            for (EstatisticaTime et : tabela.values()) {
                et.saldoGols = et.golsPro - et.golsContra;
            }

            List<EstatisticaTime> classificacao = new ArrayList<>(tabela.values());
            classificacao.sort((a, b) -> {
                if (b.pontos != a.pontos) return Integer.compare(b.pontos, a.pontos);
                if (b.vitorias != a.vitorias) return Integer.compare(b.vitorias, a.vitorias);
                if (b.saldoGols != a.saldoGols) return Integer.compare(b.saldoGols, a.saldoGols);
                return a.nomeTime.compareToIgnoreCase(b.nomeTime);
            });

            String formato = req.getParameter("formato");
            if ("csv".equalsIgnoreCase(formato)) {
                exportarCsv(resp, classificacao);
            } else {
                JsonUtil.enviar(resp, classificacao);
            }

        } catch (SQLException e) {
            JsonUtil.enviarErro(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Erro ao acessar o banco de dados: " + e.getMessage());
        } catch (NumberFormatException e) {
            JsonUtil.enviarErro(resp, HttpServletResponse.SC_BAD_REQUEST, "modalidadeId inválido.");
        }
    }


    private void exportarCsv(HttpServletResponse resp, List<EstatisticaTime> dados) throws IOException {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("text/csv;charset=UTF-8");
        resp.setHeader("Content-Disposition", "attachment; filename=classificacao-interclasse.csv");
        // BOM para o Excel reconhecer UTF-8 corretamente.
        resp.getWriter().write("\uFEFFposicao,time,pontos,vitorias,empates,derrotas,golsPro,golsContra,saldoGols\r\n");
        int posicao = 1;
        for (EstatisticaTime item : dados) {
            resp.getWriter().write(posicao++ + ",\"" + csv(item.nomeTime) + "\"," + item.pontos + "," + item.vitorias + "," + item.empates + "," + item.derrotas + "," + item.golsPro + "," + item.golsContra + "," + item.saldoGols + "\r\n");
        }
        resp.getWriter().flush();
    }

    private String csv(String valor) {
        return valor == null ? "" : valor.replace("\"", "\"\"");
    }

    public static class EstatisticaTime {
        public int timeId;
        public String nomeTime;
        public int pontos = 0;
        public int vitorias = 0;
        public int empates = 0;
        public int derrotas = 0;
        public int golsPro = 0;
        public int golsContra = 0;
        public int saldoGols = 0;

        public EstatisticaTime(int timeId, String nomeTime) {
            this.timeId = timeId;
            this.nomeTime = nomeTime;
        }
    }
}