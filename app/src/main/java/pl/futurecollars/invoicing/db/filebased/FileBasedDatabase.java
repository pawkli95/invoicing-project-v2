package pl.futurecollars.invoicing.db.filebased;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Data;
import org.springframework.stereotype.Repository;
import pl.futurecollars.invoicing.config.FilePathConfig;
import pl.futurecollars.invoicing.db.Database;
import pl.futurecollars.invoicing.model.Invoice;
import pl.futurecollars.invoicing.utils.FileService;
import pl.futurecollars.invoicing.utils.JsonService;

@Data
@Repository
public class FileBasedDatabase implements Database {

    private final JsonService jsonService;
    private final FileService jsonFileService;
    private final FileService idsFileService;

    public FileBasedDatabase(JsonService jsonService) {
        this.jsonService = jsonService;
        this.jsonFileService = new FileService(FilePathConfig.JSON_FILE);
        this.idsFileService = new FileService(FilePathConfig.IDS_FILE);
    }

    @Override
    public Invoice save(Invoice invoice) {
        if (invoice != null) {
            makeSureInvoiceIdIsUnique(invoice);
            writeInvoiceToDatabase(invoice);
            return invoice;
        }
        return null;
    }

    private void makeSureInvoiceIdIsUnique(Invoice invoice) {
        List<String> idsUsed = getIdList();
        while (idsUsed.contains(invoice.getId().toString())) {
            invoice.setId(UUID.randomUUID());
        }
    }

    private List<String> getIdList() {
        return idsFileService.read();
    }

    private void writeInvoiceToDatabase(Invoice invoice) {
        String jsonString = jsonService.toJsonString(invoice);
        jsonFileService.write(jsonString);
        idsFileService.write(invoice.getId().toString());
    }

    @Override
    public Invoice getById(UUID id) throws NoSuchElementException {
        if (invoiceIdIsInDatabase(id)) {
            return jsonFileService.read().stream()
                    .map(jsonService::toObject)
                    .filter(invoice -> invoice.getId().equals(id)).collect(Collectors.toList()).get(0);
        }
        throw new NoSuchElementException();
    }

    public boolean invoiceIdIsInDatabase(UUID id) {
        List<String> idsUsed = getIdList();
        return idsUsed.contains(id.toString());
    }

    @Override
    public List<Invoice> getAll() {
        return jsonFileService.read().stream()
                .map(jsonService::toObject)
                .collect(Collectors.toList());
    }

    @Override
    public Invoice update(Invoice updatedInvoice) {
        if (updatedInvoice != null && invoiceIsInDatabase(updatedInvoice)) {
            updateInvoice(updatedInvoice);
            return updatedInvoice;
        }
        return null;
    }

    private boolean invoiceIsInDatabase(Invoice invoice) {
        return getIdList().contains(invoice.getId().toString());
    }

    private void updateInvoice(Invoice updatedInvoice) {
        ArrayList<Invoice> invoiceList = new ArrayList<>(getAll());
        invoiceList.removeIf(invoice -> invoice.getId().equals(updatedInvoice.getId()));
        invoiceList.add(updatedInvoice);
        jsonFileService.eraseFile();
        invoiceList.stream()
                .map(jsonService::toJsonString)
                .forEach(jsonFileService::write);
    }

    @Override
    public boolean delete(UUID id) throws NoSuchElementException {
        if (invoiceIdIsInDatabase(id)) {
            deleteInvoice(id);
            deleteId(id);
            return true;
        }
        throw new NoSuchElementException();
    }

    private void deleteInvoice(UUID id) {
        ArrayList<Invoice> invoiceList = new ArrayList<>(getAll());
        invoiceList.removeIf(invoice -> invoice.getId().equals(id));
        jsonFileService.eraseFile();
        invoiceList.stream()
                .map(jsonService::toJsonString)
                .forEach(jsonFileService::write);
    }

    private void deleteId(UUID id) {
        ArrayList<String> ids = new ArrayList<>(getIdList());
        ids.removeIf(stringId -> stringId.equals(id.toString()));
        idsFileService.eraseFile();
        ids.forEach(idsFileService::write);
    }
}