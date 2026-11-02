package com.prism.api.datasources;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.prism.common.logging.LogCategory;
import com.prism.common.logging.PerformanceLog;

/**
 * /api/data-sources: Metrics → Data Sources, per enterprise. Errors are {success:false, message}: 400 for an invalid
 * request, 404 for a data source the enterprise doesn't have, 409 for a name in use or a data source that still has
 * facts or dimensions.
 * TODO (roadmap #4): the enterprise comes from the {@code enterprisePk} parameter until sessions exist.
 */
@RestController
@RequestMapping("/api/data-sources")
public class DataSourcesController {

    private static final Logger log = LogCategory.CONTROLLER.logger(DataSourcesController.class);

    private final DataSourcesService svc;

    public DataSourcesController(DataSourcesService svc) {
        this.svc = svc;
    }

    /** The enterprise's data sources by name, each with its fact, dimension and metric counts. */
    @GetMapping
    public ResponseEntity<Object> list(@RequestParam("enterprisePk") long enterprisePk) {
        log.debug("Started");
        return PerformanceLog.time("DataSourcesController.list", () -> handle(() -> {
            List<DataSource> sources = svc.list(enterprisePk);
            return ResponseEntity.ok(sources);
        }));
    }

    /**
     * What the data source pages need to know about this server: {dev_connections_allowed, properties:[{name, label,
     * secret, required, driver, hint}]} (the settings a development-only data source may hold).
     */
    @GetMapping("/options")
    public ResponseEntity<Object> options() {
        log.debug("Started");
        return PerformanceLog.time("DataSourcesController.options", () -> handle(() -> ResponseEntity.ok(svc.options())));
    }

    /** One data source; 404 if the enterprise has none with that key. */
    @GetMapping("/{dataSourcePk}")
    public ResponseEntity<Object> get(@RequestParam("enterprisePk") long enterprisePk,
                                      @PathVariable("dataSourcePk") int dataSourcePk) {
        log.debug("Started");
        return PerformanceLog.time("DataSourcesController.get", () -> handle(() -> svc.get(enterprisePk, dataSourcePk)
                .<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElseGet(() -> error(HttpStatus.NOT_FOUND, "Data source not found."))));
    }

    /** Creates a data source; 200 with the saved data source. */
    @PostMapping
    public ResponseEntity<Object> create(@RequestParam("enterprisePk") long enterprisePk,
                                         @RequestBody DataSourceRequest request) {
        log.debug("Started");
        return PerformanceLog.time("DataSourcesController.create",
                () -> handle(() -> ResponseEntity.ok(svc.create(enterprisePk, request))));
    }

    /** Replaces a data source's settings; 200 with the saved data source, 404 if not the enterprise's. */
    @PutMapping("/{dataSourcePk}")
    public ResponseEntity<Object> update(@RequestParam("enterprisePk") long enterprisePk,
                                         @PathVariable("dataSourcePk") int dataSourcePk,
                                         @RequestBody DataSourceRequest request) {
        log.debug("Started");
        return PerformanceLog.time("DataSourcesController.update", () -> handle(() ->
                svc.update(enterprisePk, dataSourcePk, request)
                        .<ResponseEntity<Object>>map(ResponseEntity::ok)
                        .orElseGet(() -> error(HttpStatus.NOT_FOUND, "Data source not found."))));
    }

    /** Deletes a data source with no facts or dimensions; 204, 404, or 409 while metadata still belongs to it. */
    @DeleteMapping("/{dataSourcePk}")
    public ResponseEntity<Object> delete(@RequestParam("enterprisePk") long enterprisePk,
                                         @PathVariable("dataSourcePk") int dataSourcePk) {
        log.debug("Started");
        return PerformanceLog.time("DataSourcesController.delete", () -> handle(() ->
                svc.delete(enterprisePk, dataSourcePk)
                        ? ResponseEntity.noContent().build()
                        : error(HttpStatus.NOT_FOUND, "Data source not found.")));
    }

    /**
     * Test connection: 200 {success, message, tables:[{kind, name, table, readable, error}]} (success false when
     * Prism can't read every registered table, or can't connect to this kind of source yet); 404 if not the
     * enterprise's.
     */
    @PostMapping("/{dataSourcePk}/test")
    public ResponseEntity<Object> test(@RequestParam("enterprisePk") long enterprisePk,
                                       @PathVariable("dataSourcePk") int dataSourcePk) {
        log.debug("Started");
        return PerformanceLog.time("DataSourcesController.test", () -> handle(() -> svc.test(enterprisePk, dataSourcePk)
                .<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElseGet(() -> error(HttpStatus.NOT_FOUND, "Data source not found."))));
    }

    private static ResponseEntity<Object> handle(Supplier<ResponseEntity<Object>> work) {
        log.debug("Started");
        try {
            return work.get();
        } catch (DataSourcesService.DuplicateNameException | DataSourcesService.InUseException e) {
            return error(HttpStatus.CONFLICT, e.getMessage());
        } catch (DuplicateKeyException e) {
            // Two saves with the same name at once: the unique constraint (SQL/013) catches the second.
            return error(HttpStatus.CONFLICT, "A data source with that name already exists.");
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private static ResponseEntity<Object> error(HttpStatus status, String message) {
        log.debug("Started");
        return ResponseEntity.status(status).body(Map.of("success", false, "message", message));
    }
}
