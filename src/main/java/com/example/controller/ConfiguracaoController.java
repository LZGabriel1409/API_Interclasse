package com.example.controller;

import com.example.dao.ConfiguracaoDao;
import com.example.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

@WebServlet("/admin/configuracoes")
public class ConfiguracaoController extends HttpServlet {

    private final ConfiguracaoDao configuracaoDao = new ConfiguracaoDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        Map<String, String> configs = configuracaoDao.listarTodas();
        resp.getWriter().write(JsonUtil.toJson(configs));
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        atualizarConfiguracoes(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        atualizarConfiguracoes(req, resp);
    }

    private void atualizarConfiguracoes(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        // Converte o JSON recebido (ex: {"PONTOS_VITORIA": "3", "PERMITE_EMPATE": "0"}) em Map
        @SuppressWarnings("unchecked")
        Map<String, Object> dados = JsonUtil.fromJson(sb.toString(), Map.class);

        if (dados == null || dados.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"erro\": \"Nenhuma configuração informada.\"}");
            return;
        }

        for (Map.Entry<String, Object> entry : dados.entrySet()) {
            String chave = entry.getKey();
            String valor = String.valueOf(entry.getValue());
            configuracaoDao.atualizar(chave, valor);
        }

        resp.getWriter().write("{\"mensagem\": \"Configurações atualizadas com sucesso.\"}");
    }
}