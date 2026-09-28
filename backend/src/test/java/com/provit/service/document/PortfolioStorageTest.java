package com.provit.service.document;

import static org.junit.Assert.*;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.provit.common.file.FileCategory;
import com.provit.dao.document.DocumentDAO;
import com.provit.dto.document.PortfolioCreateRequestDTO;
import com.provit.dto.document.PortfolioDTO;
import com.provit.service.document.impl.DocumentServiceImpl;
import com.provit.service.document.storage.impl.LocalPortfolioFileStorage;
import com.provit.service.file.impl.FileUploadServiceImpl;

public class PortfolioStorageTest {
    @Rule public TemporaryFolder directory = new TemporaryFolder();
    private final byte[] pdf = "%PDF-1.7\nfixture".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

    @Test
    public void newFilesUseUniqueUuidNamesOutsidePublicDirectory() throws Exception {
        Path publicRoot = directory.newFolder("public").toPath();
        Path privateRoot = directory.newFolder("private").toPath();
        var storage = new LocalPortfolioFileStorage(privateRoot.toString(), publicRoot.toString());
        Path first = Path.of(storage.store(15, file()));
        Path second = Path.of(storage.store(15, file()));
        assertEquals(privateRoot, first.getParent());
        assertTrue(first.getFileName().toString().matches("[a-f0-9-]{36}\\.pdf"));
        assertNotEquals(first, second);
        assertArrayEquals(pdf, read(storage, first));
        storage.deleteIfExists(first.toString());
        assertFalse(Files.exists(first));
        assertTrue(Files.exists(second));
        assertThrows(IllegalArgumentException.class, () ->
                new LocalPortfolioFileStorage(publicRoot.resolve("portfolio_uploadfile").toString(), publicRoot.toString()));
    }

    @Test
    public void legacyNumericFilesRemainReadableAndDeletableButOutsidePathsAreRejected() throws Exception {
        Path publicRoot = directory.newFolder("public").toPath();
        Path privateRoot = directory.newFolder("private").toPath();
        Path legacy = Files.createDirectories(publicRoot.resolve("portfolio_uploadfile")).resolve("15.pdf");
        Files.write(legacy, pdf);
        Path outside = directory.newFile("outside.pdf").toPath();
        Files.write(outside, pdf);
        var storage = new LocalPortfolioFileStorage(privateRoot.toString(), publicRoot.toString());
        assertArrayEquals(pdf, read(storage, legacy));
        assertThrows(IllegalStateException.class, () -> storage.loadAsResource(outside.toString()));
        assertThrows(IllegalStateException.class, () -> storage.deleteIfExists(outside.toString()));
        assertThrows(IllegalStateException.class, () -> storage.loadAsResource(privateRoot.resolve("../outside.pdf").toString()));
        assertTrue(Files.exists(outside));
        storage.deleteIfExists(legacy.toString());
        assertFalse(Files.exists(legacy));
    }

    @Test
    public void uploadKeepsOriginalAndSavedNamesSeparateAndRollbackRemovesOnlyNewFile() throws Exception {
        Path publicRoot = directory.newFolder("public").toPath();
        Path privateRoot = directory.newFolder("private").toPath();
        var storage = new LocalPortfolioFileStorage(privateRoot.toString(), publicRoot.toString());
        AtomicReference<PortfolioDTO> inserted = new AtomicReference<>();
        DocumentDAO dao = dao((name, args) -> {
            if (name.equals("selectNextPortfolioNum")) return 15;
            if (name.equals("insertPortfolio")) { inserted.set((PortfolioDTO) args[0]); return 1; }
            throw new AssertionError(name);
        });
        var service = new DocumentServiceImpl(dao, storage);
        var request = new PortfolioCreateRequestDTO();
        request.setPortfolioTitle("Sample");
        request.setFile(new MockMultipartFile("file", "C:\\fakepath\\원본.pdf", "application/pdf", pdf));
        TransactionSynchronizationManager.initSynchronization();
        try {
            PortfolioDTO created = service.createPortfolio(7, request);
            assertSame(created, inserted.get());
            assertEquals(7, created.getUserNum());
            assertEquals("원본.pdf", created.getOriginalFileName());
            assertEquals(Path.of(created.getFileUrl()).getFileName().toString(), created.getSavedFileName());
            assertNotEquals(created.getOriginalFileName(), created.getSavedFileName());
            assertTrue(Files.exists(Path.of(created.getFileUrl())));
            for (var callback : TransactionSynchronizationManager.getSynchronizations()) {
                callback.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            }
            assertFalse(Files.exists(Path.of(created.getFileUrl())));
        } finally { TransactionSynchronizationManager.clearSynchronization(); }
    }

    @Test
    public void ownerDeleteRemovesPdfOnlyAfterCommitAndRollbackKeepsIt() throws Exception {
        Path publicRoot = directory.newFolder("public").toPath();
        Path privateRoot = directory.newFolder("private").toPath();
        var storage = new LocalPortfolioFileStorage(privateRoot.toString(), publicRoot.toString());
        PortfolioDTO portfolio = new PortfolioDTO();
        portfolio.setPortfolioNum(15);
        portfolio.setUserNum(7);
        portfolio.setFileUrl(storage.store(15, file()));
        DocumentDAO dao = dao((name, args) -> {
            if (name.equals("selectPortfolioByPortfolioNum")) return portfolio;
            if (name.equals("deletePortfolio")) { assertEquals(7, args[0]); assertEquals(15, args[1]); return 1; }
            throw new AssertionError(name);
        });
        var service = new DocumentServiceImpl(dao, storage);
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.deletePortfolio(7, 15);
            assertTrue(Files.exists(Path.of(portfolio.getFileUrl())));
            for (var callback : TransactionSynchronizationManager.getSynchronizations()) {
                callback.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            }
            assertTrue(Files.exists(Path.of(portfolio.getFileUrl())));
        } finally { TransactionSynchronizationManager.clearSynchronization(); }
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.deletePortfolio(7, 15);
            for (var callback : TransactionSynchronizationManager.getSynchronizations()) callback.afterCommit();
            assertFalse(Files.exists(Path.of(portfolio.getFileUrl())));
        } finally { TransactionSynchronizationManager.clearSynchronization(); }
    }

    @Test
    public void publicUploadApiCannotCreateOrDeletePortfolioFilesEvenAsAdmin() {
        var uploadService = new FileUploadServiceImpl(null);
        assertThrows(IllegalArgumentException.class, () ->
                uploadService.uploadFile(file(), FileCategory.PORTFOLIO, 15, 7, "ADMIN"));
        assertThrows(IllegalArgumentException.class, () ->
                uploadService.deleteFile(FileCategory.PORTFOLIO, "15_random_sample.pdf", 7, "ADMIN"));
    }

    private MockMultipartFile file() { return new MockMultipartFile("file", "sample.pdf", "application/pdf", pdf); }

    private byte[] read(LocalPortfolioFileStorage storage, Path file) throws java.io.IOException {
        try (var input = storage.loadAsResource(file.toString()).getInputStream()) { return input.readAllBytes(); }
    }

    private DocumentDAO dao(DAOCall call) {
        return (DocumentDAO) Proxy.newProxyInstance(DocumentDAO.class.getClassLoader(),
                new Class<?>[] { DocumentDAO.class }, (proxy, method, args) -> call.run(method.getName(), args));
    }
    private interface DAOCall { Object run(String name, Object[] args); }
}
