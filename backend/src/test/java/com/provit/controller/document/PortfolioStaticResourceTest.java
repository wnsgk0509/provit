package com.provit.controller.document;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.core.io.UrlResource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import org.w3c.dom.Element;

public class PortfolioStaticResourceTest {
    @Rule public TemporaryFolder directory = new TemporaryFolder();

    @Test
    public void realResourceMappingsHideExistingPortfoliosAndPreservePostAndProfileImages() throws Exception {
        Path publicRoot = directory.newFolder("public").toPath();
        Files.write(Files.createDirectories(publicRoot.resolve("portfolio_uploadfile")).resolve("15.pdf"), new byte[] { 1 });
        Files.write(Files.createDirectories(publicRoot.resolve("post_uploadfile")).resolve("sample.png"), new byte[] { 2 });
        Files.write(Files.createDirectories(publicRoot.resolve("profile_uploadfile")).resolve("sample.png"), new byte[] { 3 });

        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        var xml = factory.newDocumentBuilder().parse(Path.of("src/main/webapp/WEB-INF/spring/appServlet/servlet-context.xml").toFile());
        var entries = xml.getElementsByTagNameNS("http://www.springframework.org/schema/mvc", "resources");
        var servlet = new MockServletContext();
        try (var context = new GenericWebApplicationContext()) {
            context.setServletContext(servlet);
            Map<String, Object> handlers = new LinkedHashMap<>();
            for (int index = 0; index < entries.getLength(); index++) {
                Element entry = (Element) entries.item(index);
                String mapping = entry.getAttribute("mapping");
                if (!mapping.startsWith("/uploads/")) continue;
                String location = entry.getAttribute("location").replace(
                        "file:${file.upload.base-dir:D:/fileStorage_Provit}/", publicRoot.toUri().toString());
                var handler = new ResourceHttpRequestHandler();
                handler.setServletContext(servlet);
                handler.setLocations(java.util.List.of(new UrlResource(location)));
                handler.afterPropertiesSet();
                handlers.put(mapping, handler);
            }
            var mapping = new SimpleUrlHandlerMapping();
            mapping.setUrlMap(handlers);
            context.registerBean("resourceMappings", SimpleUrlHandlerMapping.class, () -> mapping);
            context.refresh();
            var mvc = MockMvcBuilders.webAppContextSetup(context).build();
            assertEquals(404, mvc.perform(get("/uploads/portfolio_uploadfile/15.pdf")).andReturn().getResponse().getStatus());
            assertEquals(404, mvc.perform(head("/uploads/portfolio_uploadfile/15.pdf")).andReturn().getResponse().getStatus());
            assertEquals(404, mvc.perform(get("/uploads/profile_uploadfile/../portfolio_uploadfile/15.pdf")).andReturn().getResponse().getStatus());
            assertEquals(200, mvc.perform(get("/uploads/post_uploadfile/sample.png")).andReturn().getResponse().getStatus());
            assertEquals(200, mvc.perform(get("/uploads/profile_uploadfile/sample.png")).andReturn().getResponse().getStatus());
        }
    }
}
