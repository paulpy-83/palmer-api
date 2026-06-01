package com.palmar.palmer.api.service;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;
import com.palmar.palmer.api.config.SambaProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class SambaService {

    private final SambaProperties props;
    private final ReentrantLock lock = new ReentrantLock();

    private SMBClient  client;
    private Connection connection;
    private Session    session;
    private DiskShare  share;

    // ── Ciclo de vida ────────────────────────────────────────────────────────

    @PostConstruct
    void connect() {
        lock.lock();
        try {
            openConnection();
        } catch (Exception e) {
            log.warn("No se pudo conectar a Samba al arrancar (se reintentará en el primer request): {}", e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    @PreDestroy
    void disconnect() {
        lock.lock();
        try {
            closeQuietly();
            log.info("Conexión Samba cerrada.");
        } finally {
            lock.unlock();
        }
    }

    // ── API pública ──────────────────────────────────────────────────────────

    /**
     * Descarga la imagen desde el share SMB y la guarda en caché (Caffeine).
     * Reutiliza la conexión SMB persistente; reconecta automáticamente si cayó.
     * TTL y tamaño máximo configurables via cache.imagenes.* en application.yaml.
     */
    @Cacheable(value = "imagenes", key = "#filename", unless = "#result == null || #result.length == 0")
    public Optional<byte[]> fetchImage(String filename) {
        String remotePath = buildRemotePath(filename);
        log.debug("Cache miss — descargando imagen desde Samba: '{}'", remotePath);

        lock.lock();
        try {
            ensureConnected();

            if (!share.fileExists(remotePath)) {
                log.debug("Archivo no encontrado en Samba: '{}'", remotePath);
                return Optional.empty();
            }

            try (File smbFile = share.openFile(
                         remotePath,
                         EnumSet.of(AccessMask.GENERIC_READ), null,
                         SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null);
                 InputStream stream = smbFile.getInputStream()) {
                byte[] bytes = stream.readAllBytes();
                log.debug("Imagen descargada: '{}' ({} bytes)", remotePath, bytes.length);
                return Optional.of(bytes);
            }

        } catch (IOException e) {
            log.error("Error leyendo archivo Samba '{}': {}", remotePath, e.getMessage(), e);
            invalidateConnection();
            return Optional.empty();
        } finally {
            lock.unlock();
        }
    }

    // ── Gestión de conexión ──────────────────────────────────────────────────

    /**
     * Verifica que la conexión, sesión y share estén activos.
     * Si alguno falló, cierra todo y reconecta desde cero.
     */
    private void ensureConnected() throws IOException {
        boolean needsReconnect = share == null
                || connection == null || !connection.isConnected()
                || session == null;

        if (needsReconnect) {
            log.info("Reconectando a Samba ({}\\{})...", props.getHost(), props.getShare());
            closeQuietly();
            openConnection();
        }
    }

    private void openConnection() throws IOException {
        client     = new SMBClient();
        connection = client.connect(props.getHost());
        AuthenticationContext auth = new AuthenticationContext(
                props.getUser(), props.getPassword().toCharArray(), props.getDomain());
        session = connection.authenticate(auth);
        share   = (DiskShare) session.connectShare(props.getShare());
        log.info("Conexión Samba establecida: {}\\{}", props.getHost(), props.getShare());
    }

    private void invalidateConnection() {
        log.warn("Invalidando conexión Samba — se reconectará en el próximo request.");
        closeQuietly();
    }

    private void closeQuietly() {
        for (AutoCloseable resource : new AutoCloseable[]{ share, session, connection, client }) {
            if (resource != null) {
                try { resource.close(); } catch (Exception ignored) {}
            }
        }
        share = null; session = null; connection = null; client = null;
    }

    // ── Utilidades ───────────────────────────────────────────────────────────

    private String buildRemotePath(String filename) {
        String folder = props.getFolder();
        if (folder == null || folder.isBlank()) return filename;
        return folder.replace("/", "\\") + "\\" + filename;
    }
}
