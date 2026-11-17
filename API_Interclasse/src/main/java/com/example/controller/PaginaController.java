package com.example.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.example.util.JsonUtil;

/**
 * Serve a pagina web do interclasse em /api/app.
 *
 * O ApiController esta mapeado em "/" (servlet padrao) e por isso captura
 * qualquer URL, inclusive arquivos estaticos. A pagina fica entao em
 * WEB-INF/pagina.html (que o cliente nao acessa direto) e este servlet a
 * entrega na mesma origem da API, para que o cookie de sessao funcione.
 */
@WebServlet({ "/app", "/app/" })
public class PaginaController extends HttpServlet {

    private static final String ARQUIVO = "/WEB-INF/pagina.html";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try (InputStream pagina = getServletContext().getResourceAsStream(ARQUIVO)) {
            if (pagina == null) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_NOT_FOUND, "pagina nao encontrada");
                return;
            }

            response.setContentType("text/html;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);

            OutputStream out = response.getOutputStream();
            byte[] buffer = new byte[4096];
            int lidos;
            while ((lidos = pagina.read(buffer)) != -1) {
                out.write(buffer, 0, lidos);
            }
            out.flush();
        }
    }
}
